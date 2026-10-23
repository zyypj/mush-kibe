package br.com.muxe.core.common.messaging.envelope;

import br.com.muxe.core.common.messaging.codec.PacketCodecException;
import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.security.EnvelopeSigner;
import java.util.Arrays;
import java.util.UUID;

public final class EnvelopeCodec {

    public static final int MAGIC = 0x4D58;
    public static final int PROTOCOL_VERSION = 1;
    public static final int DEFAULT_MAX_BYTES = 256 * 1024;

    private static final int HEADER_BYTES = 3;

    private final EnvelopeSigner signer;
    private final int maxBytes;

    public EnvelopeCodec(EnvelopeSigner signer) {
        this(signer, DEFAULT_MAX_BYTES);
    }

    public EnvelopeCodec(EnvelopeSigner signer, int maxBytes) {
        this.signer = signer;
        this.maxBytes = maxBytes;
    }

    public byte[] encode(PacketEnvelope envelope) {
        PacketWriter out = new PacketWriter(envelope.payload().length + 128);
        out.writeShort(MAGIC)
            .writeByte(PROTOCOL_VERSION)
            .writeByte(envelope.kind().code())
            .writeUuid(envelope.packetId())
            .writeString(envelope.typeId())
            .writeVarInt(envelope.typeVersion())
            .writeString(envelope.origin())
            .writeNullableUuid(envelope.correlationId())
            .writeLong(envelope.sentAtMillis())
            .writeBytes(envelope.payload());
        byte[] body = out.toByteArray();
        byte[] signature = signer.sign(body);
        int total = body.length + signature.length;
        if (total > maxBytes) {
            throw new EnvelopeRejectedException(RejectionReason.TOO_LARGE, envelope.typeKey() + " is " + total + " bytes, limit is " + maxBytes);
        }
        byte[] message = Arrays.copyOf(body, total);
        System.arraycopy(signature, 0, message, body.length, signature.length);
        return message;
    }

    public PacketEnvelope decode(byte[] message) {
        int signatureLength = signer.signatureLength();
        if (message.length > maxBytes) {
            throw new EnvelopeRejectedException(RejectionReason.TOO_LARGE, "Envelope of " + message.length + " bytes exceeds " + maxBytes);
        }
        if (message.length < HEADER_BYTES + signatureLength) {
            throw new EnvelopeRejectedException(RejectionReason.MALFORMED, "Envelope too short");
        }
        int magic = ((message[0] & 0xFF) << 8) | (message[1] & 0xFF);
        if (magic != MAGIC) {
            throw new EnvelopeRejectedException(RejectionReason.BAD_MAGIC, "Not a Muxe envelope");
        }
        int protocol = message[2] & 0xFF;
        if (protocol != PROTOCOL_VERSION) {
            throw new EnvelopeRejectedException(RejectionReason.UNSUPPORTED_PROTOCOL, "Protocol " + protocol + " is not supported");
        }
        int bodyLength = message.length - signatureLength;
        byte[] body = Arrays.copyOf(message, bodyLength);
        byte[] signature = Arrays.copyOfRange(message, bodyLength, message.length);
        if (!signer.verify(body, signature)) {
            throw new EnvelopeRejectedException(RejectionReason.BAD_SIGNATURE, "Envelope signature does not match");
        }
        try {
            return parse(new PacketReader(body, HEADER_BYTES, bodyLength - HEADER_BYTES));
        } catch (PacketCodecException exception) {
            throw new EnvelopeRejectedException(RejectionReason.MALFORMED, exception.getMessage(), exception);
        }
    }

    private static PacketEnvelope parse(PacketReader in) {
        EnvelopeKind kind = EnvelopeKind.fromCode(in.readByte());
        UUID packetId = in.readUuid();
        String typeId = in.readString();
        int typeVersion = in.readVarInt();
        String origin = in.readString();
        UUID correlationId = in.readNullableUuid();
        long sentAt = in.readLong();
        byte[] payload = in.readBytes();
        in.ensureFullyRead();
        try {
            return new PacketEnvelope(packetId, kind, typeId, typeVersion, origin, correlationId, sentAt, payload);
        } catch (IllegalArgumentException exception) {
            throw new EnvelopeRejectedException(RejectionReason.MALFORMED, exception.getMessage(), exception);
        }
    }
}

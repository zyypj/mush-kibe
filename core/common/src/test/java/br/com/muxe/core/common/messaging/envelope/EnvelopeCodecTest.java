package br.com.muxe.core.common.messaging.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.messaging.security.HmacEnvelopeSigner;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnvelopeCodecTest {

    private static final String SECRET = "first-secret-with-at-least-32-bytes!";
    private static final String OTHER_SECRET = "other-secret-with-at-least-32-bytes!";

    private final EnvelopeCodec codec = new EnvelopeCodec(HmacEnvelopeSigner.of(SECRET));

    @Test
    void roundTripsEveryField() {
        PacketEnvelope envelope = request();

        PacketEnvelope decoded = codec.decode(codec.encode(envelope));

        assertThat(decoded.packetId()).isEqualTo(envelope.packetId());
        assertThat(decoded.kind()).isEqualTo(EnvelopeKind.REQUEST);
        assertThat(decoded.typeId()).isEqualTo("player.locate_request");
        assertThat(decoded.typeVersion()).isEqualTo(1);
        assertThat(decoded.origin()).isEqualTo("lobby-1");
        assertThat(decoded.correlationId()).isEqualTo(envelope.correlationId());
        assertThat(decoded.sentAtMillis()).isEqualTo(1_700_000_000_000L);
        assertThat(decoded.payload()).containsExactly(9, 8, 7);
    }

    @Test
    void rejectsEnvelopesSignedWithAnotherSecret() {
        byte[] forged = new EnvelopeCodec(HmacEnvelopeSigner.of(OTHER_SECRET)).encode(request());

        assertRejected(forged, RejectionReason.BAD_SIGNATURE);
    }

    @Test
    void rejectsTamperedPayloads() {
        byte[] message = codec.encode(request());
        message[message.length - 40] ^= 0x01;

        assertRejected(message, RejectionReason.BAD_SIGNATURE);
    }

    @Test
    void acceptsPreviousSecretsDuringRotation() {
        byte[] signedWithOldSecret = new EnvelopeCodec(HmacEnvelopeSigner.of(OTHER_SECRET)).encode(request());
        EnvelopeCodec rotating = new EnvelopeCodec(HmacEnvelopeSigner.of(SECRET, List.of(OTHER_SECRET)));

        assertThat(rotating.decode(signedWithOldSecret).origin()).isEqualTo("lobby-1");
    }

    @Test
    void rejectsForeignMessages() {
        byte[] garbage = new byte[64];

        assertRejected(garbage, RejectionReason.BAD_MAGIC);
    }

    @Test
    void rejectsUnsupportedProtocolVersions() {
        byte[] message = codec.encode(request());
        message[2] = 99;

        assertRejected(message, RejectionReason.UNSUPPORTED_PROTOCOL);
    }

    @Test
    void refusesToEncodeOversizedEnvelopes() {
        EnvelopeCodec small = new EnvelopeCodec(HmacEnvelopeSigner.of(SECRET), 1024);
        PacketEnvelope huge = new PacketEnvelope(UUID.randomUUID(), EnvelopeKind.MESSAGE, "test.huge", 1, "lobby-1", null, 0, new byte[2048]);

        assertThatThrownBy(() -> small.encode(huge))
            .isInstanceOfSatisfying(EnvelopeRejectedException.class, exception -> assertThat(exception.reason()).isEqualTo(RejectionReason.TOO_LARGE));
    }

    @Test
    void rejectsSecretsThatAreTooShort() {
        assertThatThrownBy(() -> HmacEnvelopeSigner.of("short")).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertRejected(byte[] message, RejectionReason reason) {
        assertThatThrownBy(() -> codec.decode(message))
            .isInstanceOfSatisfying(EnvelopeRejectedException.class, exception -> assertThat(exception.reason()).isEqualTo(reason));
    }

    private static PacketEnvelope request() {
        return new PacketEnvelope(
            UUID.randomUUID(),
            EnvelopeKind.REQUEST,
            "player.locate_request",
            1,
            "lobby-1",
            UUID.randomUUID(),
            1_700_000_000_000L,
            new byte[] {9, 8, 7}
        );
    }
}

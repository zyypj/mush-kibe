package br.com.muxe.core.common.messaging.codec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class PacketWriter {

    public static final int MAX_STRING_BYTES = 32_767;
    public static final int MAX_COLLECTION_SIZE = 65_536;

    private byte[] buffer;
    private int size;

    public PacketWriter() {
        this(64);
    }

    public PacketWriter(int initialCapacity) {
        this.buffer = new byte[Math.max(16, initialCapacity)];
    }

    public PacketWriter writeByte(int value) {
        ensureCapacity(1);
        buffer[size++] = (byte) value;
        return this;
    }

    public PacketWriter writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    public PacketWriter writeShort(int value) {
        ensureCapacity(2);
        buffer[size++] = (byte) (value >>> 8);
        buffer[size++] = (byte) value;
        return this;
    }

    public PacketWriter writeInt(int value) {
        ensureCapacity(4);
        for (int shift = 24; shift >= 0; shift -= 8) {
            buffer[size++] = (byte) (value >>> shift);
        }
        return this;
    }

    public PacketWriter writeLong(long value) {
        ensureCapacity(8);
        for (int shift = 56; shift >= 0; shift -= 8) {
            buffer[size++] = (byte) (value >>> shift);
        }
        return this;
    }

    public PacketWriter writeVarInt(int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        return writeByte(remaining);
    }

    public PacketWriter writeString(String value) {
        if (value == null) {
            throw new PacketCodecException("String cannot be null, use writeNullableString");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new PacketCodecException("String too long: " + bytes.length + " bytes");
        }
        writeVarInt(bytes.length);
        return writeRaw(bytes, 0, bytes.length);
    }

    public PacketWriter writeNullableString(String value) {
        writeBoolean(value != null);
        return value == null ? this : writeString(value);
    }

    public PacketWriter writeUuid(UUID value) {
        if (value == null) {
            throw new PacketCodecException("UUID cannot be null, use writeNullableUuid");
        }
        writeLong(value.getMostSignificantBits());
        return writeLong(value.getLeastSignificantBits());
    }

    public PacketWriter writeNullableUuid(UUID value) {
        writeBoolean(value != null);
        return value == null ? this : writeUuid(value);
    }

    public PacketWriter writeBytes(byte[] value) {
        writeVarInt(value.length);
        return writeRaw(value, 0, value.length);
    }

    public <E extends Enum<E>> PacketWriter writeEnum(E value) {
        return writeString(value.name());
    }

    public <T> PacketWriter writeList(Collection<T> values, BiConsumer<PacketWriter, T> elementWriter) {
        if (values.size() > MAX_COLLECTION_SIZE) {
            throw new PacketCodecException("Collection too large: " + values.size());
        }
        writeVarInt(values.size());
        for (T value : values) {
            elementWriter.accept(this, value);
        }
        return this;
    }

    public PacketWriter writeRaw(byte[] bytes, int offset, int length) {
        ensureCapacity(length);
        System.arraycopy(bytes, offset, buffer, size, length);
        size += length;
        return this;
    }

    public int size() {
        return size;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, size);
    }

    private void ensureCapacity(int extra) {
        int required = size + extra;
        if (required > buffer.length) {
            buffer = Arrays.copyOf(buffer, Math.max(required, buffer.length * 2));
        }
    }
}

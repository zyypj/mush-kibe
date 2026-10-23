package br.com.muxe.core.common.messaging.codec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class PacketReader {

    private final byte[] data;
    private final int limit;
    private int position;

    public PacketReader(byte[] data) {
        this(data, 0, data.length);
    }

    public PacketReader(byte[] data, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new PacketCodecException("Invalid reader bounds");
        }
        this.data = data;
        this.position = offset;
        this.limit = offset + length;
    }

    public int readByte() {
        require(1);
        return data[position++] & 0xFF;
    }

    public boolean readBoolean() {
        int value = readByte();
        if (value > 1) {
            throw new PacketCodecException("Invalid boolean: " + value);
        }
        return value == 1;
    }

    public int readShort() {
        require(2);
        int value = ((data[position] & 0xFF) << 8) | (data[position + 1] & 0xFF);
        position += 2;
        return value;
    }

    public int readInt() {
        require(4);
        int value = 0;
        for (int index = 0; index < 4; index++) {
            value = (value << 8) | (data[position++] & 0xFF);
        }
        return value;
    }

    public long readLong() {
        require(8);
        long value = 0;
        for (int index = 0; index < 8; index++) {
            value = (value << 8) | (data[position++] & 0xFF);
        }
        return value;
    }

    public int readVarInt() {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int current = readByte();
            value |= (current & 0x7F) << shift;
            if ((current & 0x80) == 0) {
                return value;
            }
        }
        throw new PacketCodecException("VarInt too long");
    }

    public String readString() {
        int length = readVarInt();
        if (length < 0 || length > PacketWriter.MAX_STRING_BYTES) {
            throw new PacketCodecException("Invalid string length: " + length);
        }
        require(length);
        String value = new String(data, position, length, StandardCharsets.UTF_8);
        position += length;
        return value;
    }

    public String readNullableString() {
        return readBoolean() ? readString() : null;
    }

    public UUID readUuid() {
        return new UUID(readLong(), readLong());
    }

    public UUID readNullableUuid() {
        return readBoolean() ? readUuid() : null;
    }

    public byte[] readBytes() {
        int length = readVarInt();
        if (length < 0) {
            throw new PacketCodecException("Invalid byte array length: " + length);
        }
        return readRaw(length);
    }

    public byte[] readRaw(int length) {
        require(length);
        byte[] value = Arrays.copyOfRange(data, position, position + length);
        position += length;
        return value;
    }

    public <E extends Enum<E>> E readEnum(Class<E> type) {
        String name = readString();
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException exception) {
            throw new PacketCodecException("Unknown " + type.getSimpleName() + " constant: " + name, exception);
        }
    }

    public <T> List<T> readList(Function<PacketReader, T> elementReader) {
        int size = readVarInt();
        if (size < 0 || size > PacketWriter.MAX_COLLECTION_SIZE) {
            throw new PacketCodecException("Invalid collection size: " + size);
        }
        List<T> values = new ArrayList<>(Math.min(size, 256));
        for (int index = 0; index < size; index++) {
            values.add(elementReader.apply(this));
        }
        return values;
    }

    public int remaining() {
        return limit - position;
    }

    public void ensureFullyRead() {
        if (remaining() != 0) {
            throw new PacketCodecException(remaining() + " unread bytes left in packet");
        }
    }

    private void require(int bytes) {
        if (bytes < 0 || position + bytes > limit) {
            throw new PacketCodecException("Packet truncated: needed " + bytes + " bytes, " + remaining() + " left");
        }
    }
}

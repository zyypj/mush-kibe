package br.com.muxe.minigame.engine.world.nbt;

import br.com.muxe.minigame.engine.world.nbt.NbtTag.ByteArrayTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ByteTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.CompoundTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.DoubleTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.FloatTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntArrayTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ListTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.LongTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ShortTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.StringTag;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NbtIo {

    private static final int MAX_DEPTH = 512;

    private NbtIo() {
    }

    public static CompoundTag read(DataInput input) throws IOException {
        byte type = input.readByte();
        if (type != NbtTag.COMPOUND) {
            throw new IOException("The root tag must be a compound, got type " + type);
        }
        input.readUTF();
        return (CompoundTag) readPayload(input, type, 0);
    }

    public static void write(DataOutput output, CompoundTag root) throws IOException {
        output.writeByte(NbtTag.COMPOUND);
        output.writeUTF("");
        writePayload(output, root);
    }

    public static byte[] toBytes(CompoundTag root) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            write(output, root);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return bytes.toByteArray();
    }

    public static CompoundTag fromBytes(byte[] bytes) {
        try {
            return read(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static NbtTag readPayload(DataInput input, byte type, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("NBT is nested deeper than " + MAX_DEPTH);
        }
        return switch (type) {
            case NbtTag.BYTE -> new ByteTag(input.readByte());
            case NbtTag.SHORT -> new ShortTag(input.readShort());
            case NbtTag.INT -> new IntTag(input.readInt());
            case NbtTag.LONG -> new LongTag(input.readLong());
            case NbtTag.FLOAT -> new FloatTag(input.readFloat());
            case NbtTag.DOUBLE -> new DoubleTag(input.readDouble());
            case NbtTag.BYTE_ARRAY -> {
                byte[] value = new byte[length(input)];
                input.readFully(value);
                yield new ByteArrayTag(value);
            }
            case NbtTag.STRING -> new StringTag(input.readUTF());
            case NbtTag.LIST -> {
                byte elementType = input.readByte();
                int size = length(input);
                List<NbtTag> values = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    values.add(readPayload(input, elementType, depth + 1));
                }
                yield new ListTag(elementType, values);
            }
            case NbtTag.COMPOUND -> {
                Map<String, NbtTag> values = new LinkedHashMap<>();
                while (true) {
                    byte childType = input.readByte();
                    if (childType == NbtTag.END) {
                        break;
                    }
                    String name = input.readUTF();
                    values.put(name, readPayload(input, childType, depth + 1));
                }
                yield new CompoundTag(values);
            }
            case NbtTag.INT_ARRAY -> {
                int[] value = new int[length(input)];
                for (int index = 0; index < value.length; index++) {
                    value[index] = input.readInt();
                }
                yield new IntArrayTag(value);
            }
            default -> throw new IOException("Unknown NBT tag type " + type);
        };
    }

    private static void writePayload(DataOutput output, NbtTag tag) throws IOException {
        switch (tag) {
            case ByteTag value -> output.writeByte(value.value());
            case ShortTag value -> output.writeShort(value.value());
            case IntTag value -> output.writeInt(value.value());
            case LongTag value -> output.writeLong(value.value());
            case FloatTag value -> output.writeFloat(value.value());
            case DoubleTag value -> output.writeDouble(value.value());
            case ByteArrayTag value -> {
                output.writeInt(value.value().length);
                output.write(value.value());
            }
            case StringTag value -> output.writeUTF(value.value());
            case ListTag value -> {
                output.writeByte(value.values().isEmpty() ? NbtTag.END : value.elementType());
                output.writeInt(value.values().size());
                for (NbtTag element : value.values()) {
                    writePayload(output, element);
                }
            }
            case CompoundTag value -> {
                for (Map.Entry<String, NbtTag> entry : value.values().entrySet()) {
                    output.writeByte(entry.getValue().type());
                    output.writeUTF(entry.getKey());
                    writePayload(output, entry.getValue());
                }
                output.writeByte(NbtTag.END);
            }
            case IntArrayTag value -> {
                output.writeInt(value.value().length);
                for (int element : value.value()) {
                    output.writeInt(element);
                }
            }
        }
    }

    private static int length(DataInput input) throws IOException {
        int length = input.readInt();
        if (length < 0) {
            throw new IOException("Negative NBT length " + length);
        }
        return length;
    }
}

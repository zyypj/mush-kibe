package br.com.muxe.minigame.engine.world.nbt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public sealed interface NbtTag {

    byte END = 0;
    byte BYTE = 1;
    byte SHORT = 2;
    byte INT = 3;
    byte LONG = 4;
    byte FLOAT = 5;
    byte DOUBLE = 6;
    byte BYTE_ARRAY = 7;
    byte STRING = 8;
    byte LIST = 9;
    byte COMPOUND = 10;
    byte INT_ARRAY = 11;

    byte type();

    record ByteTag(byte value) implements NbtTag {
        public byte type() {
            return BYTE;
        }
    }

    record ShortTag(short value) implements NbtTag {
        public byte type() {
            return SHORT;
        }
    }

    record IntTag(int value) implements NbtTag {
        public byte type() {
            return INT;
        }
    }

    record LongTag(long value) implements NbtTag {
        public byte type() {
            return LONG;
        }
    }

    record FloatTag(float value) implements NbtTag {
        public byte type() {
            return FLOAT;
        }
    }

    record DoubleTag(double value) implements NbtTag {
        public byte type() {
            return DOUBLE;
        }
    }

    record ByteArrayTag(byte[] value) implements NbtTag {
        public byte type() {
            return BYTE_ARRAY;
        }
    }

    record StringTag(String value) implements NbtTag {
        public byte type() {
            return STRING;
        }
    }

    record ListTag(byte elementType, List<NbtTag> values) implements NbtTag {

        public ListTag {
            values = List.copyOf(values);
        }

        public byte type() {
            return LIST;
        }
    }

    record IntArrayTag(int[] value) implements NbtTag {
        public byte type() {
            return INT_ARRAY;
        }
    }

    record CompoundTag(Map<String, NbtTag> values) implements NbtTag {

        public CompoundTag {
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        public byte type() {
            return COMPOUND;
        }

        public Optional<NbtTag> get(String key) {
            return Optional.ofNullable(values.get(key));
        }

        public Optional<CompoundTag> compound(String key) {
            return get(key).filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast);
        }

        public List<NbtTag> list(String key) {
            return get(key).filter(ListTag.class::isInstance).map(tag -> ((ListTag) tag).values()).orElse(List.of());
        }

        public Optional<byte[]> bytes(String key) {
            return get(key).filter(ByteArrayTag.class::isInstance).map(tag -> ((ByteArrayTag) tag).value());
        }

        public Optional<int[]> ints(String key) {
            return get(key).filter(IntArrayTag.class::isInstance).map(tag -> ((IntArrayTag) tag).value());
        }

        public int intValue(String key, int fallback) {
            return get(key).map(tag -> switch (tag) {
                case ByteTag value -> (int) value.value();
                case ShortTag value -> (int) value.value();
                case IntTag value -> value.value();
                default -> fallback;
            }).orElse(fallback);
        }

        public CompoundTag with(String key, NbtTag value) {
            Map<String, NbtTag> copy = new LinkedHashMap<>(values);
            copy.put(key, value);
            return new CompoundTag(copy);
        }
    }
}

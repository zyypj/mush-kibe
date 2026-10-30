package br.com.muxe.minigame.engine.world.map;

import br.com.muxe.minigame.engine.world.nbt.NbtIo;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.CompoundTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntTag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ChunkData(int x, int z, List<SectionData> sections, byte[] biomes, int[] heightMap, List<byte[]> tileEntities) {

    public static final int COLUMNS = 256;

    public ChunkData {
        sections = sections.stream().filter(section -> !section.isEmpty()).sorted(Comparator.comparingInt(SectionData::y)).toList();
        if (sections.stream().map(SectionData::y).distinct().count() != sections.size()) {
            throw new IllegalArgumentException("Chunk " + x + "," + z + " has two sections at the same height");
        }
        Objects.requireNonNull(biomes, "biomes");
        Objects.requireNonNull(heightMap, "heightMap");
        if (biomes.length != COLUMNS || heightMap.length != COLUMNS) {
            throw new IllegalArgumentException("Chunk " + x + "," + z + " has biome or height arrays of the wrong size");
        }
        tileEntities = List.copyOf(tileEntities);
    }

    public static long key(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    public static int keyX(long key) {
        return (int) (key >> 32);
    }

    public static int keyZ(long key) {
        return (int) key;
    }

    public long key() {
        return key(x, z);
    }

    public boolean isEmpty() {
        return sections.isEmpty() && tileEntities.isEmpty();
    }

    public Optional<SectionData> section(int y) {
        return sections.stream().filter(section -> section.y() == y).findFirst();
    }

    public ChunkData shiftedBy(int chunksX, int chunksZ) {
        if (chunksX == 0 && chunksZ == 0) {
            return this;
        }
        List<byte[]> moved = new ArrayList<>(tileEntities.size());
        for (byte[] tile : tileEntities) {
            CompoundTag nbt = NbtIo.fromBytes(tile);
            nbt = nbt.with("x", new IntTag(nbt.intValue("x", 0) + chunksX * 16)).with("z", new IntTag(nbt.intValue("z", 0) + chunksZ * 16));
            moved.add(NbtIo.toBytes(nbt));
        }
        return new ChunkData(x + chunksX, z + chunksZ, sections, biomes, heightMap, moved);
    }
}

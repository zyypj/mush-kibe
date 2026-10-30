package br.com.muxe.minigame.engine.world.map;

import br.com.muxe.minigame.engine.world.nbt.NbtIo;
import br.com.muxe.minigame.engine.world.nbt.NbtTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.CompoundTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.StringTag;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MapFixtures {

    public static final int WOOL = 35;
    public static final int SIGN = 63;

    private MapFixtures() {
    }

    public static SectionData section(int y, int id, int data) {
        char[] blocks = new char[SectionData.BLOCKS];
        Arrays.fill(blocks, SectionData.block(id, data));
        byte[] skyLight = new byte[SectionData.NIBBLES];
        Arrays.fill(skyLight, (byte) 0xFF);
        return new SectionData(y, blocks, new byte[SectionData.NIBBLES], skyLight);
    }

    public static ChunkData chunk(int x, int z, SectionData... sections) {
        byte[] biomes = new byte[ChunkData.COLUMNS];
        Arrays.fill(biomes, (byte) 1);
        int[] heightMap = new int[ChunkData.COLUMNS];
        Arrays.fill(heightMap, 64);
        return new ChunkData(x, z, List.of(sections), biomes, heightMap, List.of());
    }

    public static ChunkData withSign(ChunkData chunk, String text) {
        Map<String, NbtTag> sign = new LinkedHashMap<>();
        sign.put("id", new StringTag("Sign"));
        sign.put("x", new IntTag(chunk.x() * 16));
        sign.put("y", new IntTag(65));
        sign.put("z", new IntTag(chunk.z() * 16));
        sign.put("Text1", new StringTag(text));
        byte[] tile = NbtIo.toBytes(new CompoundTag(sign));
        return new ChunkData(chunk.x(), chunk.z(), chunk.sections(), chunk.biomes(), chunk.heightMap(), List.of(tile));
    }
}

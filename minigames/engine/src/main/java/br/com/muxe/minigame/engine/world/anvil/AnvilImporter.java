package br.com.muxe.minigame.engine.world.anvil;

import br.com.muxe.minigame.engine.world.map.ChunkData;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import br.com.muxe.minigame.engine.world.map.SectionData;
import br.com.muxe.minigame.engine.world.nbt.NbtIo;
import br.com.muxe.minigame.engine.world.nbt.NbtTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.CompoundTag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class AnvilImporter {

    private static final byte PLAINS = 1;

    private AnvilImporter() {
    }

    public static boolean isWorldFolder(Path folder) {
        return Files.isDirectory(folder.resolve("region"));
    }

    public static MapTemplate importWorld(Path folder) throws IOException {
        Path regions = folder.resolve("region");
        if (!Files.isDirectory(regions)) {
            throw new IOException(folder + " has no region folder");
        }
        MapTemplate.Builder template = MapTemplate.builder();
        List<Path> files;
        try (Stream<Path> listing = Files.list(regions)) {
            files = listing.filter(file -> file.getFileName().toString().endsWith(".mca")).sorted().toList();
        }
        for (Path file : files) {
            for (CompoundTag root : RegionFile.read(file)) {
                root.compound("Level").map(AnvilImporter::chunk).ifPresent(template::put);
            }
        }
        return template.build();
    }

    static ChunkData chunk(CompoundTag level) {
        List<SectionData> sections = new ArrayList<>();
        for (NbtTag tag : level.list("Sections")) {
            if (tag instanceof CompoundTag section) {
                sections.add(section(section));
            }
        }
        byte[] biomes = level.bytes("Biomes").filter(value -> value.length == ChunkData.COLUMNS).orElseGet(() -> filled(PLAINS));
        int[] heightMap = level.ints("HeightMap").filter(value -> value.length == ChunkData.COLUMNS).orElseGet(() -> new int[ChunkData.COLUMNS]);
        List<byte[]> tiles = new ArrayList<>();
        for (NbtTag tag : level.list("TileEntities")) {
            if (tag instanceof CompoundTag tile) {
                tiles.add(NbtIo.toBytes(tile));
            }
        }
        return new ChunkData(level.intValue("xPos", 0), level.intValue("zPos", 0), sections, biomes, heightMap, tiles);
    }

    private static SectionData section(CompoundTag section) {
        byte[] ids = section.bytes("Blocks").filter(value -> value.length == SectionData.BLOCKS).orElseGet(() -> new byte[SectionData.BLOCKS]);
        byte[] add = section.bytes("Add").filter(value -> value.length == SectionData.NIBBLES).orElse(null);
        byte[] data = section.bytes("Data").filter(value -> value.length == SectionData.NIBBLES).orElseGet(() -> new byte[SectionData.NIBBLES]);
        char[] blocks = new char[SectionData.BLOCKS];
        for (int index = 0; index < SectionData.BLOCKS; index++) {
            int id = ids[index] & 0xFF;
            if (add != null) {
                id |= nibble(add, index) << 8;
            }
            blocks[index] = SectionData.block(id, nibble(data, index));
        }
        byte[] blockLight = section.bytes("BlockLight").filter(value -> value.length == SectionData.NIBBLES).orElseGet(() -> new byte[SectionData.NIBBLES]);
        byte[] skyLight = section.bytes("SkyLight").filter(value -> value.length == SectionData.NIBBLES).orElseGet(() -> nibbles(15));
        return new SectionData(section.intValue("Y", 0), blocks, blockLight, skyLight);
    }

    static int nibble(byte[] array, int index) {
        int value = array[index >> 1];
        return (index & 1) == 0 ? value & 15 : value >> 4 & 15;
    }

    private static byte[] filled(byte value) {
        byte[] array = new byte[ChunkData.COLUMNS];
        Arrays.fill(array, value);
        return array;
    }

    private static byte[] nibbles(int value) {
        byte[] array = new byte[SectionData.NIBBLES];
        Arrays.fill(array, (byte) (value << 4 | value));
        return array;
    }
}

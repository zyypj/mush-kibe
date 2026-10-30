package br.com.muxe.minigame.engine.world.anvil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.engine.world.map.ChunkData;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import br.com.muxe.minigame.engine.world.map.SectionData;
import br.com.muxe.minigame.engine.world.nbt.NbtIo;
import br.com.muxe.minigame.engine.world.nbt.NbtTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ByteArrayTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ByteTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.CompoundTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntArrayTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.IntTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.ListTag;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.StringTag;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnvilImporterTest {

    private static final int STAINED_CLAY = 159;
    private static final int HIGH_ID = 300;

    @TempDir
    Path world;

    @Test
    void aWorldFolderBecomesAMapWithItsBlocksAndTileEntities() throws IOException {
        byte[] blocks = new byte[SectionData.BLOCKS];
        byte[] data = new byte[SectionData.NIBBLES];
        byte[] add = new byte[SectionData.NIBBLES];
        blocks[SectionData.index(0, 0, 0)] = (byte) STAINED_CLAY;
        data[0] = 0x0B;
        blocks[SectionData.index(1, 0, 0)] = (byte) (HIGH_ID & 0xFF);
        add[0] = (byte) ((HIGH_ID >> 8) << 4);
        CompoundTag sign = tile("Sign", 33, 70, -15);
        writeRegion("r.1.-1.mca", 2, List.of(
            level(33, -15, List.of(section(4, blocks, data, add)), List.of(sign)),
            level(34, -15, List.of(), List.of())
        ));

        MapTemplate template = AnvilImporter.importWorld(world);

        assertThat(template.chunkCount()).isEqualTo(1);
        ChunkData chunk = template.chunk(33, -15).orElseThrow();
        SectionData section = chunk.section(4).orElseThrow();
        assertThat(section.id(0, 0, 0)).isEqualTo(STAINED_CLAY);
        assertThat(section.data(0, 0, 0)).isEqualTo(11);
        assertThat(section.id(1, 0, 0)).isEqualTo(HIGH_ID);
        assertThat(section.data(1, 0, 0)).isZero();
        assertThat(chunk.biomes()).containsOnly((byte) 4);
        assertThat(chunk.tileEntities()).hasSize(1);
        assertThat(NbtIo.fromBytes(chunk.tileEntities().getFirst()).intValue("x", 0)).isEqualTo(33);
    }

    @Test
    void gzipChunksAreReadToo() throws IOException {
        byte[] blocks = new byte[SectionData.BLOCKS];
        blocks[7] = 1;
        writeRegion("r.0.0.mca", 1, List.of(level(0, 0, List.of(section(0, blocks, new byte[SectionData.NIBBLES], null)), List.of())));

        assertThat(AnvilImporter.importWorld(world).chunk(0, 0).orElseThrow().section(0).orElseThrow().id(7, 0, 0)).isEqualTo(1);
    }

    @Test
    void aFolderWithoutRegionsIsNotAWorld() {
        assertThat(AnvilImporter.isWorldFolder(world)).isFalse();
        assertThatThrownBy(() -> AnvilImporter.importWorld(world)).isInstanceOf(IOException.class).hasMessageContaining("region");
    }

    @Test
    void lowNibblesComeFirst() {
        byte[] nibbles = {(byte) 0xA3};

        assertThat(AnvilImporter.nibble(nibbles, 0)).isEqualTo(3);
        assertThat(AnvilImporter.nibble(nibbles, 1)).isEqualTo(10);
    }

    private CompoundTag level(int x, int z, List<NbtTag> sections, List<NbtTag> tiles) {
        byte[] biomes = new byte[ChunkData.COLUMNS];
        Arrays.fill(biomes, (byte) 4);
        Map<String, NbtTag> level = new LinkedHashMap<>();
        level.put("xPos", new IntTag(x));
        level.put("zPos", new IntTag(z));
        level.put("Sections", new ListTag(NbtTag.COMPOUND, sections));
        level.put("Biomes", new ByteArrayTag(biomes));
        level.put("HeightMap", new IntArrayTag(new int[ChunkData.COLUMNS]));
        level.put("Entities", new ListTag(NbtTag.COMPOUND, List.of(tile("ArmorStand", x * 16, 70, z * 16))));
        level.put("TileEntities", new ListTag(NbtTag.COMPOUND, tiles));
        return new CompoundTag(Map.of("Level", new CompoundTag(level)));
    }

    private static NbtTag section(int y, byte[] blocks, byte[] data, byte[] add) {
        Map<String, NbtTag> section = new LinkedHashMap<>();
        section.put("Y", new ByteTag((byte) y));
        section.put("Blocks", new ByteArrayTag(blocks));
        section.put("Data", new ByteArrayTag(data));
        if (add != null) {
            section.put("Add", new ByteArrayTag(add));
        }
        section.put("BlockLight", new ByteArrayTag(new byte[SectionData.NIBBLES]));
        section.put("SkyLight", new ByteArrayTag(new byte[SectionData.NIBBLES]));
        return new CompoundTag(section);
    }

    private static CompoundTag tile(String id, int x, int y, int z) {
        Map<String, NbtTag> tile = new LinkedHashMap<>();
        tile.put("id", new StringTag(id));
        tile.put("x", new IntTag(x));
        tile.put("y", new IntTag(y));
        tile.put("z", new IntTag(z));
        return new CompoundTag(tile);
    }

    private void writeRegion(String name, int compression, List<CompoundTag> chunks) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        ByteBuffer header = ByteBuffer.allocate(RegionFile.SECTOR * 2);
        int sector = 2;
        for (int index = 0; index < chunks.size(); index++) {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (OutputStream stream = compression == 1 ? new GZIPOutputStream(compressed) : new DeflaterOutputStream(compressed);
                 DataOutputStream output = new DataOutputStream(stream)) {
                NbtIo.write(output, chunks.get(index));
            }
            byte[] payload = compressed.toByteArray();
            int sectors = (payload.length + 5 + RegionFile.SECTOR - 1) / RegionFile.SECTOR;
            header.putInt(index * 4, sector << 8 | sectors);
            ByteBuffer chunk = ByteBuffer.allocate(sectors * RegionFile.SECTOR);
            chunk.putInt(payload.length + 1).put((byte) compression).put(payload);
            body.write(chunk.array());
            sector += sectors;
        }
        Path regions = Files.createDirectories(world.resolve("region"));
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(header.array());
        file.write(body.toByteArray());
        Files.write(regions.resolve(name), file.toByteArray());
    }
}

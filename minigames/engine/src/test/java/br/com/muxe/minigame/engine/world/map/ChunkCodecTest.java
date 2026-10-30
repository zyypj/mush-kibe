package br.com.muxe.minigame.engine.world.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.engine.world.nbt.NbtIo;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ChunkCodecTest {

    @Test
    void aChunkComesBackWithItsBlocksLightBiomesHeightsAndTileEntities() {
        SectionData floor = MapFixtures.section(4, MapFixtures.WOOL, 11);
        floor.blocks()[SectionData.index(3, 2, 9)] = SectionData.block(MapFixtures.SIGN, 4);
        floor.blockLight()[100] = 0x5A;
        ChunkData chunk = MapFixtures.withSign(MapFixtures.chunk(-3, 7, floor), "&9Azul");

        ChunkData read = ChunkCodec.decompress(ChunkCodec.compress(chunk));

        assertThat(read.x()).isEqualTo(-3);
        assertThat(read.z()).isEqualTo(7);
        assertThat(read.sections()).hasSize(1);
        SectionData section = read.section(4).orElseThrow();
        assertThat(section.id(0, 0, 0)).isEqualTo(MapFixtures.WOOL);
        assertThat(section.data(0, 0, 0)).isEqualTo(11);
        assertThat(section.id(3, 2, 9)).isEqualTo(MapFixtures.SIGN);
        assertThat(section.data(3, 2, 9)).isEqualTo(4);
        assertThat(section.blockLight()[100]).isEqualTo((byte) 0x5A);
        assertThat(section.skyLight()).containsOnly((byte) 0xFF);
        assertThat(read.biomes()).containsOnly((byte) 1);
        assertThat(read.heightMap()).containsOnly(64);
        assertThat(read.tileEntities()).hasSize(1);
        assertThat(NbtIo.fromBytes(read.tileEntities().getFirst()).get("Text1").orElseThrow().toString()).contains("&9Azul");
    }

    @Test
    void aMostlyUniformChunkIsStoredInAFractionOfItsSize() {
        ChunkData chunk = MapFixtures.chunk(0, 0, MapFixtures.section(3, 1, 0), MapFixtures.section(4, MapFixtures.WOOL, 14));

        byte[] compressed = ChunkCodec.compress(chunk);

        assertThat(compressed.length).isLessThan(ChunkCodec.encode(chunk).length / 50);
    }

    @Test
    void emptySectionsAreNeverStored() {
        ChunkData chunk = MapFixtures.chunk(1, 1, MapFixtures.section(2, 0, 0), MapFixtures.section(5, 1, 0));

        assertThat(chunk.sections()).extracting(SectionData::y).containsExactly(5);
        assertThat(ChunkCodec.decompress(ChunkCodec.compress(chunk)).sections()).extracting(SectionData::y).containsExactly(5);
    }

    @Test
    void corruptedDataFailsWithAClearMessage() {
        byte[] compressed = ChunkCodec.compress(MapFixtures.chunk(0, 0, MapFixtures.section(0, 1, 0)));
        byte[] truncated = Arrays.copyOf(compressed, compressed.length / 2);

        assertThatThrownBy(() -> ChunkCodec.decompress(truncated)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Corrupted chunk");
    }

    @Test
    void twoSectionsAtTheSameHeightAreRejected() {
        assertThatThrownBy(() -> MapFixtures.chunk(0, 0, MapFixtures.section(1, 1, 0), MapFixtures.section(1, 2, 0)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

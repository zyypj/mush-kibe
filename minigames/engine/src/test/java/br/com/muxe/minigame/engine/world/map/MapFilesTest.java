package br.com.muxe.minigame.engine.world.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapFilesTest {

    @TempDir
    Path folder;

    @Test
    void aMapIsWrittenAndReadBackChunkByChunk() throws IOException {
        MapTemplate template = MapTemplate.builder()
            .put(MapFixtures.chunk(-2, 5, MapFixtures.section(4, MapFixtures.WOOL, 11)))
            .put(MapFixtures.chunk(3, -1, MapFixtures.section(3, 1, 0)))
            .build();
        Path file = folder.resolve("maps").resolve("1v1_slip" + MapFiles.EXTENSION);

        MapFiles.write(file, template);
        MapTemplate read = MapFiles.read(file);

        assertThat(read.chunkCount()).isEqualTo(2);
        assertThat(read.chunk(-2, 5).orElseThrow().section(4).orElseThrow().data(0, 0, 0)).isEqualTo(11);
        assertThat(read.chunk(3, -1).orElseThrow().section(3).orElseThrow().id(5, 5, 5)).isEqualTo(1);
        assertThat(read.chunk(0, 0)).isEmpty();
        assertThat(read.bounds()).isEqualTo(new ChunkBounds(-2, -1, 3, 5));
        assertThat(Files.list(file.getParent())).containsExactly(file);
    }

    @Test
    void anEmptyChunkRemovesTheChunkFromTheMap() {
        MapTemplate template = MapTemplate.builder()
            .put(MapFixtures.chunk(0, 0, MapFixtures.section(4, 1, 0)))
            .put(MapFixtures.chunk(0, 0))
            .build();

        assertThat(template.chunkCount()).isZero();
        assertThat(template.bounds().isEmpty()).isTrue();
    }

    @Test
    void aFileThatIsNotAMapIsRejected() throws IOException {
        Path file = folder.resolve("level.dat");
        Files.write(file, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9});

        assertThatThrownBy(() -> MapFiles.read(file)).isInstanceOf(IOException.class).hasMessageContaining("not a Muxe map");
    }

    @Test
    void theTemplateKeepsOnlyCompressedChunksInMemory() {
        MapTemplate template = MapTemplate.builder().put(MapFixtures.chunk(0, 0, MapFixtures.section(4, 1, 0), MapFixtures.section(5, 1, 0))).build();

        assertThat(template.compressedBytes()).isLessThan(2 * (SectionData.BLOCKS * 2L) / 50);
    }
}

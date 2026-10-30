package br.com.muxe.minigame.engine.world.map;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EditableMapTest {

    @Test
    void editsReachTheSnapshotAndEmptiedChunksLeaveIt() {
        EditableMap map = new EditableMap(MapTemplate.builder().put(MapFixtures.chunk(0, 0, MapFixtures.section(4, 1, 0))).build());

        map.save(MapFixtures.chunk(2, 3, MapFixtures.section(5, MapFixtures.WOOL, 3)));
        map.save(MapFixtures.chunk(0, 0));

        MapTemplate snapshot = map.snapshot();
        assertThat(snapshot.keys()).containsExactly(ChunkData.key(2, 3));
        assertThat(map.load(2, 3).orElseThrow().section(5).orElseThrow().data(0, 0, 0)).isEqualTo(3);
        assertThat(map.load(0, 0)).isEmpty();
    }
}

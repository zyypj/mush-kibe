package br.com.muxe.minigame.engine.world.cell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.engine.world.map.ChunkData;
import br.com.muxe.minigame.engine.world.map.MapFixtures;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import br.com.muxe.minigame.engine.world.nbt.NbtIo;
import org.junit.jupiter.api.Test;

class MatchCellsTest {

    private static final int SIZE = 64;

    private final MatchCells cells = new MatchCells(SIZE, 4, 12);
    private final MapTemplate map = MapTemplate.builder()
        .put(MapFixtures.withSign(MapFixtures.chunk(-1, 2, MapFixtures.section(4, MapFixtures.WOOL, 11)), "&9Azul"))
        .put(MapFixtures.chunk(3, -5, MapFixtures.section(3, 1, 0)))
        .build();

    @Test
    void eachMatchGetsItsOwnCellInAGrid() {
        Cell first = cells.allocate(map);
        Cell second = cells.allocate(map);
        Cell fifth = allocateUntil(4);

        assertThat(first).isEqualTo(new Cell(0, 0, 0, SIZE));
        assertThat(second).isEqualTo(new Cell(1, SIZE, 0, SIZE));
        assertThat(fifth).isEqualTo(new Cell(4, 0, SIZE, SIZE));
        assertThat(first.offsetChunkX()).isEqualTo(32);
        assertThat(cells.active()).isEqualTo(5);
    }

    @Test
    void theMapIsServedAroundTheCellCentreWithItsTileEntitiesMoved() {
        Cell cell = cells.allocate(map);
        cells.allocate(map);

        ChunkData chunk = cells.load(cell.offsetChunkX() - 1, cell.offsetChunkZ() + 2).orElseThrow();

        assertThat(chunk.x()).isEqualTo(31);
        assertThat(chunk.z()).isEqualTo(34);
        assertThat(chunk.section(4).orElseThrow().data(0, 0, 0)).isEqualTo(11);
        assertThat(NbtIo.fromBytes(chunk.tileEntities().getFirst()).intValue("x", 0)).isEqualTo(31 * 16);
        assertThat(NbtIo.fromBytes(chunk.tileEntities().getFirst()).intValue("z", 0)).isEqualTo(34 * 16);
        assertThat(cells.load(cell.offsetChunkX(), cell.offsetChunkZ())).isEmpty();
        assertThat(cells.load(SIZE * 3 + 32, 32)).isEmpty();
        assertThat(cells.load(-1, 5)).isEmpty();
    }

    @Test
    void changedChunksLiveUntilTheCellIsReleasedAndTheNextMatchGetsAFreshMap() {
        Cell cell = cells.allocate(map);
        int x = cell.offsetChunkX() + 3;
        int z = cell.offsetChunkZ() - 5;
        cells.save(MapFixtures.chunk(x, z, MapFixtures.section(3, MapFixtures.WOOL, 14)));

        assertThat(cells.load(x, z).orElseThrow().section(3).orElseThrow().id(0, 0, 0)).isEqualTo(MapFixtures.WOOL);
        assertThat(cells.changedChunks(cell)).isEqualTo(1);

        cells.release(cell);
        Cell next = cells.allocate(map);

        assertThat(next.index()).isEqualTo(cell.index());
        assertThat(cells.changedChunks(next)).isZero();
        assertThat(cells.load(x, z).orElseThrow().section(3).orElseThrow().id(0, 0, 0)).isEqualTo(1);
    }

    @Test
    void anEmptyVoidChunkIsNotKeptButAnEmptiedMapChunkIs() {
        Cell cell = cells.allocate(map);

        cells.save(MapFixtures.chunk(cell.offsetChunkX() + 10, cell.offsetChunkZ() + 10));
        cells.save(MapFixtures.chunk(cell.offsetChunkX() + 3, cell.offsetChunkZ() - 5));

        assertThat(cells.changedChunks(cell)).isEqualTo(1);
        assertThat(cells.load(cell.offsetChunkX() + 3, cell.offsetChunkZ() - 5).orElseThrow().isEmpty()).isTrue();
    }

    @Test
    void savesOutsideAnyMatchAreDropped() {
        cells.save(MapFixtures.chunk(5, 5, MapFixtures.section(3, 1, 0)));

        assertThat(cells.load(5, 5)).isEmpty();
    }

    @Test
    void aMapTooWideForTheCellIsRefused() {
        MapTemplate wide = MapTemplate.builder().put(MapFixtures.chunk(21, 0, MapFixtures.section(1, 1, 0))).build();

        assertThat(cells.fits(wide)).isFalse();
        assertThatThrownBy(() -> cells.allocate(wide)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not fit");
    }

    @Test
    void blockCoordinatesConvertBetweenTheMapAndTheWorld() {
        Cell cell = allocateUntil(2);

        assertThat(cell.toWorldX(10.5)).isEqualTo(10.5 + (SIZE * 2 + 32) * 16);
        assertThat(cell.toMapX(cell.toWorldX(-7.25))).isEqualTo(-7.25);
        assertThat(cell.containsBlock(cell.toWorldX(0), cell.toWorldZ(0))).isTrue();
        assertThat(cell.containsBlock(cell.toWorldX(SIZE * 8), cell.toWorldZ(0))).isFalse();
    }

    private Cell allocateUntil(int index) {
        Cell cell;
        do {
            cell = cells.allocate(map);
        } while (cell.index() < index);
        return cell;
    }
}

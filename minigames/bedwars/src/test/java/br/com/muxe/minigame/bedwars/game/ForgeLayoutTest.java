package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.game.ForgeLayout.Cell;
import br.com.muxe.minigame.bedwars.game.ForgeLayout.Facing;
import br.com.muxe.minigame.bedwars.game.ForgeLayout.Shape;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ForgeLayoutTest {

    @Test
    void aFiveByFiveForgeDropsSixIngotsAtATimeInTheRecordedCellsFacingTheSpawn() {
        ForgeLayout layout = ForgeLayout.fiveByFive(-6, 0);

        assertThat(layout.shape()).isEqualTo(Shape.FIVE_BY_FIVE);
        assertThat(layout.facing()).isEqualTo(Facing.WEST);
        assertThat(layout.ironCells()).hasSize(24).doesNotHaveDuplicates()
            .allMatch(cell -> Math.abs(cell.dx()) <= ForgeLayout.FIVE_BY_FIVE_RADIUS && Math.abs(cell.dz()) <= ForgeLayout.FIVE_BY_FIVE_RADIUS)
            .doesNotContain(new Cell(-1, -1));
        assertThat(batch(layout, 0)).containsExactly(new Cell(0, 0), new Cell(-2, -1), new Cell(-2, -2), new Cell(2, 0), new Cell(0, -1), new Cell(2, -1));
        assertThat(batch(layout, 1)).containsExactly(new Cell(0, -2), new Cell(1, -1), new Cell(-1, -2), new Cell(2, -2), new Cell(1, -2), new Cell(-1, 2));
        assertThat(batch(layout, 2)).containsExactly(new Cell(-2, 2), new Cell(-2, 1), new Cell(1, 2), new Cell(0, 2), new Cell(0, 1), new Cell(-2, 0));
        assertThat(batch(layout, 3)).containsExactly(new Cell(-1, 1), new Cell(2, 2), new Cell(-1, 0), new Cell(2, 1), new Cell(1, 1), new Cell(1, 0));
        assertThat(batch(layout, 4)).containsExactlyElementsOf(batch(layout, 0));
    }

    @Test
    void aFiveByFiveForgeTurnsWithItsIsland() {
        ForgeLayout west = ForgeLayout.fiveByFive(-6, 0);
        ForgeLayout east = ForgeLayout.fiveByFive(6, 0);

        assertThat(east.facing()).isEqualTo(Facing.EAST);
        for (int drop = 0; drop < 24; drop++) {
            assertThat(east.cellAt(drop)).isEqualTo(new Cell(-west.cellAt(drop).dx(), -west.cellAt(drop).dz()));
        }
    }

    @Test
    void aThreeByThreeSlabFloorDropsIronInAPlusFacingTheSpawn() {
        Floor floor = new Floor(-1);
        floor.square(-1, 1, -1, 1);

        ForgeLayout layout = ForgeLayout.detect(floor, 0, 10);

        assertThat(layout.shape()).isEqualTo(Shape.THREE_BY_THREE);
        assertThat(layout.facing()).isEqualTo(Facing.SOUTH);
        assertThat(layout.ironCells()).containsExactly(new Cell(0, -1), new Cell(1, 0), new Cell(0, 0), new Cell(-1, 0), new Cell(0, 1));
    }

    @Test
    void aTwoByThreeFloorStartsAtTheFrontCentreAndNeverUsesTheBackCorners() {
        Floor floor = new Floor(0);
        floor.square(-1, 1, 0, 1);

        ForgeLayout layout = ForgeLayout.detect(floor, 0, 5);

        assertThat(layout.shape()).isEqualTo(Shape.TWO_BY_THREE);
        assertThat(layout.facing()).isEqualTo(Facing.SOUTH);
        assertThat(layout.ironCells()).hasSize(8).containsOnly(new Cell(1, 0), new Cell(-1, 0), new Cell(0, 0), new Cell(0, 1));
        assertThat(layout.ironCells().getFirst()).isEqualTo(new Cell(1, 0));
    }

    @Test
    void aTwoByThreeFloorFacingAwayFromTheSpawnIsStillFound() {
        Floor floor = new Floor(0);
        floor.square(-1, 0, -1, 1);

        ForgeLayout layout = ForgeLayout.detect(floor, 0, 5);

        assertThat(layout.shape()).isEqualTo(Shape.TWO_BY_THREE);
        assertThat(layout.facing()).isEqualTo(Facing.WEST);
        assertThat(layout.ironCells()).allSatisfy(cell -> assertThat(cell.dx()).isBetween(-1, 0));
    }

    @Test
    void withoutASlabUnderTheForgeItDropsOnOneSpot() {
        assertThat(ForgeLayout.detect(new Floor(-1), 1, 0)).isEqualTo(ForgeLayout.single());
    }

    @Test
    void aSlabCoveredByASolidBlockIsNotFloor() {
        Floor floor = new Floor(-1);
        floor.square(-1, 1, -1, 1);
        floor.solid.add(new Cell(1, 1));

        ForgeLayout layout = ForgeLayout.detect(floor, 0, 10);

        assertThat(layout.shape()).isEqualTo(Shape.TWO_BY_THREE);
    }

    @Test
    void dropsCycleThroughTheCells() {
        Floor floor = new Floor(-1);
        floor.square(-1, 1, -1, 1);
        ForgeLayout layout = ForgeLayout.detect(floor, 0, 10);

        List<Cell> four = List.of(layout.cellAt(0), layout.cellAt(1), layout.cellAt(2), layout.cellAt(3));

        assertThat(new HashSet<>(four)).hasSize(4);
        assertThat(layout.cellAt(5)).isEqualTo(layout.cellAt(0));
        assertThat(layout.cellIndexAt(7)).isEqualTo(2);
    }

    @Test
    void theClosestAxisToTheSpawnIsTheFront() {
        assertThat(Facing.closest(3, -1)).isEqualTo(Facing.EAST);
        assertThat(Facing.closest(-3, 1)).isEqualTo(Facing.WEST);
        assertThat(Facing.closest(1, -3)).isEqualTo(Facing.NORTH);
        assertThat(Facing.NORTH.right()).isEqualTo(Facing.EAST);
        assertThat(Facing.WEST.right()).isEqualTo(Facing.NORTH);
    }

    private static List<Cell> batch(ForgeLayout layout, int index) {
        return IntStream.range(index * 6, index * 6 + 6).mapToObj(layout::cellAt).toList();
    }

    private static final class Floor implements ForgeLayout.Blocks {

        private final int slabY;
        private final Set<Cell> slabs = new HashSet<>();
        private final Set<Cell> solid = new HashSet<>();

        private Floor(int slabY) {
            this.slabY = slabY;
        }

        void square(int minX, int maxX, int minZ, int maxZ) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    slabs.add(new Cell(x, z));
                }
            }
        }

        @Override
        public boolean slab(int dx, int dy, int dz) {
            return dy == slabY && slabs.contains(new Cell(dx, dz));
        }

        @Override
        public boolean solid(int dx, int dy, int dz) {
            return dy == slabY + 1 && solid.contains(new Cell(dx, dz));
        }
    }
}

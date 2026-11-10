package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.game.BedDefense.Layer;
import br.com.muxe.minigame.bedwars.game.BedDefense.Pos;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BedDefenseTest {

    private static final List<Pos> BED = List.of(new Pos(0, 64, 0), new Pos(1, 64, 0));

    @Test
    void eachLevelStacksItsLayersFromTheBedOut() {
        assertThat(BedDefense.layers(1)).containsExactly(Layer.WOOD, Layer.WOOL, Layer.GLASS);
        assertThat(BedDefense.layers(2)).containsExactly(Layer.WOOD, Layer.WOOL, Layer.END_STONE);
        assertThat(BedDefense.layers(3)).containsExactly(Layer.WOOD, Layer.WOOL, Layer.END_STONE, Layer.GLASS);
        assertThat(BedDefense.layers(0)).isEmpty();
    }

    @Test
    void theLayersFormAPyramidAroundBothHalvesOfTheBed() {
        Map<Pos, Layer> plan = BedDefense.plan(BED, 1);

        assertThat(plan).doesNotContainKeys(BED.toArray(Pos[]::new));
        assertThat(plan.get(new Pos(0, 65, 0))).isEqualTo(Layer.WOOD);
        assertThat(plan.get(new Pos(-1, 64, 0))).isEqualTo(Layer.WOOD);
        assertThat(plan.get(new Pos(2, 64, 0))).isEqualTo(Layer.WOOD);
        assertThat(plan.get(new Pos(0, 66, 0))).isEqualTo(Layer.WOOL);
        assertThat(plan.get(new Pos(-1, 64, 1))).isEqualTo(Layer.WOOL);
        assertThat(plan.get(new Pos(0, 67, 0))).isEqualTo(Layer.GLASS);
        assertThat(plan.get(new Pos(4, 64, 0))).isEqualTo(Layer.GLASS);
        assertThat(plan).doesNotContainKey(new Pos(0, 68, 0));
        assertThat(plan).doesNotContainKey(new Pos(0, 63, 0));
    }

    @Test
    void theThirdLevelAddsAnOuterGlassLayerOverEndStone() {
        Map<Pos, Layer> plan = BedDefense.plan(BED, 3);

        assertThat(plan.get(new Pos(0, 67, 0))).isEqualTo(Layer.END_STONE);
        assertThat(plan.get(new Pos(0, 68, 0))).isEqualTo(Layer.GLASS);
    }

    @Test
    void aSingleBlockTotemIsProtectedToo() {
        assertThat(BedDefense.plan(List.of(new Pos(5, 70, 5)), 2).get(new Pos(5, 71, 5))).isEqualTo(Layer.WOOD);
    }
}

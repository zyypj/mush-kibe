package br.com.muxe.minigame.api.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class LiquidFlowTest {

    @Test
    void liquidNeverWashesAwayTheBlocksOfTheMap() {
        assertThat(LiquidFlow.allowed(Material.TORCH, true, false)).isFalse();
        assertThat(LiquidFlow.allowed(Material.RED_ROSE, true, false)).isFalse();
        assertThat(LiquidFlow.allowed(Material.LONG_GRASS, true, false)).isFalse();
        assertThat(LiquidFlow.allowed(Material.REDSTONE_WIRE, true, false)).isFalse();
    }

    @Test
    void liquidStillSpreadsThroughAirAndOtherLiquids() {
        assertThat(LiquidFlow.allowed(Material.AIR, true, false)).isTrue();
        assertThat(LiquidFlow.allowed(Material.WATER, true, false)).isTrue();
        assertThat(LiquidFlow.allowed(Material.STATIONARY_LAVA, true, false)).isTrue();
    }

    @Test
    void liquidWashesAwayBlocksPlacedInTheMatchOrAnyBlockWhenTheMapCanBeBroken() {
        assertThat(LiquidFlow.allowed(Material.LADDER, true, true)).isTrue();
        assertThat(LiquidFlow.allowed(Material.TORCH, false, false)).isTrue();
    }
}

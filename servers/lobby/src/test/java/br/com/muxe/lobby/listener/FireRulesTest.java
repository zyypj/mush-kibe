package br.com.muxe.lobby.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.junit.jupiter.api.Test;

class FireRulesTest {

    @Test
    void fireNeverSpreadsButOtherBlocksStillDo() {
        assertThat(FireRules.spreads(Material.FIRE)).isFalse();
        assertThat(FireRules.spreads(Material.GRASS)).isTrue();
        assertThat(FireRules.spreads(Material.VINE)).isTrue();
    }

    @Test
    void fireNeverStartsOnItsOwn() {
        assertThat(FireRules.ignites(IgniteCause.SPREAD, false)).isFalse();
        assertThat(FireRules.ignites(IgniteCause.LAVA, false)).isFalse();
        assertThat(FireRules.ignites(IgniteCause.LIGHTNING, false)).isFalse();
        assertThat(FireRules.ignites(IgniteCause.EXPLOSION, true)).isFalse();
    }

    @Test
    void onlyStaffBuildingCanLightAFireByHand() {
        assertThat(FireRules.ignites(IgniteCause.FLINT_AND_STEEL, true)).isTrue();
        assertThat(FireRules.ignites(IgniteCause.FIREBALL, true)).isTrue();
        assertThat(FireRules.ignites(IgniteCause.FLINT_AND_STEEL, false)).isFalse();
        assertThat(FireRules.ignites(IgniteCause.SPREAD, true)).isFalse();
    }
}

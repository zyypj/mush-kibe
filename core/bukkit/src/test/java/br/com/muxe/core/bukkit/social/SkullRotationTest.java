package br.com.muxe.core.bukkit.social;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class SkullRotationTest {

    @Test
    void theHeadLooksWhereThePlayerWhoPlacedItWasLooking() {
        assertThat(SkullRotation.facing(180f)).isEqualTo(BlockFace.NORTH);
        assertThat(SkullRotation.facing(0f)).isEqualTo(BlockFace.SOUTH);
        assertThat(SkullRotation.facing(90f)).isEqualTo(BlockFace.WEST);
        assertThat(SkullRotation.facing(-90f)).isEqualTo(BlockFace.EAST);
        assertThat(SkullRotation.facing(-157.5f)).isEqualTo(BlockFace.NORTH_NORTH_EAST);
        assertThat(SkullRotation.facing(540f)).isEqualTo(BlockFace.NORTH);
    }
}

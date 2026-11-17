package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class EmptiedBucketTest {

    @Test
    void theEmptyBucketLeftByPouringDisappears() {
        assertThat(EmptiedBucket.left(new ItemStack(Material.BUCKET))).isNull();
    }

    @Test
    void onlyOneBucketOfAStackDisappears() {
        ItemStack left = EmptiedBucket.left(new ItemStack(Material.BUCKET, 3));

        assertThat(left.getType()).isEqualTo(Material.BUCKET);
        assertThat(left.getAmount()).isEqualTo(2);
    }

    @Test
    void anythingElseInTheSlotIsKept() {
        ItemStack sword = new ItemStack(Material.WOOD_SWORD);

        assertThat(EmptiedBucket.left(sword)).isSameAs(sword);
        assertThat(EmptiedBucket.left(null)).isNull();
    }
}

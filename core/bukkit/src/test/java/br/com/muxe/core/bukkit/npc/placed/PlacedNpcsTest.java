package br.com.muxe.core.bukkit.npc.placed;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.placement.domain.HeldItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PlacedNpcsTest {

    @Test
    void theItemInHandIsAOneEightItemWithItsData() {
        ItemStack wool = PlacedNpcs.hand(new HeldItem("WOOL", 14)).orElseThrow();
        ItemStack soup = PlacedNpcs.hand(new HeldItem("mushroom_soup", 0)).orElseThrow();

        assertThat(wool.getType()).isEqualTo(Material.WOOL);
        assertThat(wool.getDurability()).isEqualTo((short) 14);
        assertThat(wool.getAmount()).isEqualTo(1);
        assertThat(soup.getType()).isEqualTo(Material.MUSHROOM_SOUP);
    }

    @Test
    void unknownMaterialsAndAirHoldNothing() {
        assertThat(PlacedNpcs.hand(new HeldItem("NETHERITE_SWORD", 0))).isEmpty();
        assertThat(PlacedNpcs.hand(new HeldItem("AIR", 0))).isEmpty();
    }
}

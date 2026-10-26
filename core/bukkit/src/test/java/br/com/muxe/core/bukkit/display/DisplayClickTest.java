package br.com.muxe.core.bukkit.display;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.nms.EntityUse;
import org.junit.jupiter.api.Test;

class DisplayClickTest {

    @Test
    void aRightClickOnAnArmorStandOnlySendsInteractAt() {
        assertThat(DisplayService.countsAsClick(EntityUse.INTERACT_AT)).isTrue();
        assertThat(DisplayService.countsAsClick(EntityUse.ATTACK)).isTrue();
        assertThat(DisplayService.countsAsClick(EntityUse.INTERACT)).isFalse();
    }

    @Test
    void anEmptyHologramOwnsNoEntity() {
        assertThat(SpawnedHologram.NONE.owns(42)).isFalse();
    }
}

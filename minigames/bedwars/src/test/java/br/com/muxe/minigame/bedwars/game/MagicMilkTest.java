package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MagicMilkTest {

    private static final Instant NOW = Instant.parse("2026-11-14T22:00:00Z");

    @Test
    void drinkingShieldsFromTrapsForThirtySeconds() {
        MagicMilk milk = new MagicMilk();
        UUID player = UUID.randomUUID();

        assertThat(milk.shields(player, NOW)).isFalse();
        milk.drink(player, NOW);

        assertThat(milk.shields(player, NOW.plusSeconds(29))).isTrue();
        assertThat(milk.shields(player, NOW.plusSeconds(30))).isFalse();
        assertThat(milk.shields(UUID.randomUUID(), NOW)).isFalse();
    }

    @Test
    void onlyNegativeEffectsAreRemoved() {
        assertThat(MagicMilk.negative(List.of("SPEED", "BLINDNESS", "JUMP", "SLOW_DIGGING", "SLOW", "REGENERATION")))
            .containsExactly("BLINDNESS", "SLOW_DIGGING", "SLOW");
        assertThat(MagicMilk.negative(List.of("SPEED", "INVISIBILITY"))).isEmpty();
    }
}

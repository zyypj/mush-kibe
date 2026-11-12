package br.com.muxe.minigame.engine.setup.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SetupRulesTest {

    @Test
    void lobbyChatDoesNotReachSomeoneEditingAnArena() {
        assertThat(SetupRules.hears(null, "solo_majestic")).isFalse();
        assertThat(SetupRules.hears(null, null)).isTrue();
    }

    @Test
    void editorsOfTheSameArenaHearEachOtherOnly() {
        assertThat(SetupRules.hears("solo_majestic", "solo_majestic")).isTrue();
        assertThat(SetupRules.hears("solo_majestic", "1v1_slip")).isFalse();
        assertThat(SetupRules.hears("solo_majestic", null)).isFalse();
    }

    @Test
    void editorsMayGoBelowZeroAndOnlyFallingPastMinusSixtyCountsAsTheVoid() {
        assertThat(SetupRules.fellIntoVoid(-0.5)).isFalse();
        assertThat(SetupRules.fellIntoVoid(-59.5)).isFalse();
        assertThat(SetupRules.fellIntoVoid(-60)).isFalse();
        assertThat(SetupRules.fellIntoVoid(-60.5)).isTrue();
    }
}

package br.com.muxe.core.bukkit.buildmode;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

class BuildRulesTest {

    @Test
    void outsideBuildModeOnlyAdventureIsAllowed() {
        assertThat(BuildRules.allowsGameMode(GameMode.ADVENTURE, false)).isTrue();
        assertThat(BuildRules.allowsGameMode(GameMode.CREATIVE, false)).isFalse();
        assertThat(BuildRules.allowsGameMode(GameMode.SURVIVAL, false)).isFalse();
        assertThat(BuildRules.allowsGameMode(GameMode.SPECTATOR, false)).isFalse();
    }

    @Test
    void buildModeAllowsCreativeAndTheWayBackToAdventure() {
        assertThat(BuildRules.allowsGameMode(GameMode.CREATIVE, true)).isTrue();
        assertThat(BuildRules.allowsGameMode(GameMode.ADVENTURE, true)).isTrue();
        assertThat(BuildRules.allowsGameMode(GameMode.SURVIVAL, true)).isFalse();
    }

    @Test
    void theCommandAcceptsOnOffOrNothingToToggle() {
        assertThat(BuildRules.wants("on", false)).contains(true);
        assertThat(BuildRules.wants("OFF", true)).contains(false);
        assertThat(BuildRules.wants("", false)).contains(true);
        assertThat(BuildRules.wants("", true)).contains(false);
        assertThat(BuildRules.wants("talvez", false)).isEmpty();
    }
}

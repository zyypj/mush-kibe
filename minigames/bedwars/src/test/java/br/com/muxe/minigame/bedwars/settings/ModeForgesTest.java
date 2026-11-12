package br.com.muxe.minigame.bedwars.settings;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings.Forges;
import br.com.muxe.minigame.engine.mode.GameMode;
import org.junit.jupiter.api.Test;

class ModeForgesTest {

    private static final GameMode MEGA = new GameMode("mega", "Mega", 20, 80, 10);

    private final Forges forges = RoomFixtures.SETTINGS.forges();

    @Test
    void megaDropsSixIronEveryOnePointNineSecondsAndGoldEveryOneAndAHalfOnAFiveByFiveForge() {
        assertThat(forges.iron(MEGA)).isEqualTo(new Rate(1.9, 6, 150));
        assertThat(forges.gold("mega")).isEqualTo(new Rate(1.5, 1, 30));
        assertThat(forges.fiveByFive("mega")).isTrue();
    }

    @Test
    void theOtherModesKeepTheIslandForgeOfTheirTeamSize() {
        assertThat(forges.iron(RoomFixtures.SOLO)).isEqualTo(forges.iron(1));
        assertThat(forges.iron(RoomFixtures.DOUBLES)).isEqualTo(new Rate(0.9, 2, 48));
        assertThat(forges.gold("solo")).isEqualTo(forges.gold());
        assertThat(forges.fiveByFive("solo")).isFalse();
    }
}

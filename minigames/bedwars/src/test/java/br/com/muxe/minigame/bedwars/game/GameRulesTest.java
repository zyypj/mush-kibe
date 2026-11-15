package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import org.junit.jupiter.api.Test;

class GameRulesTest {

    @Test
    void levelsCostFiveThousandUntilNineThenFiveThousandMoreEveryTenLevels() {
        assertThat(BedWarsLevels.costOf(1)).isEqualTo(5_000);
        assertThat(BedWarsLevels.costOf(9)).isEqualTo(5_000);
        assertThat(BedWarsLevels.costOf(10)).isEqualTo(10_000);
        assertThat(BedWarsLevels.costOf(19)).isEqualTo(10_000);
        assertThat(BedWarsLevels.costOf(20)).isEqualTo(15_000);
        assertThat(BedWarsLevels.progress(0).compactNeeded()).isEqualTo("5k");
        assertThat(BedWarsLevels.progress(45_000 + 250)).isEqualTo(new BedWarsLevels.Progress(10, 250, 10_000));
        assertThat(BedWarsLevels.progress(55_000)).isEqualTo(new BedWarsLevels.Progress(11, 0, 10_000));
        assertThatThrownBy(() -> BedWarsLevels.costOf(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNewPlayerStartsAtLevelOne() {
        assertThat(BedWarsLevels.progress(0)).isEqualTo(new BedWarsLevels.Progress(1, 0, 5_000));
        assertThat(BedWarsLevels.progress(4_999).level()).isEqualTo(1);
        assertThat(BedWarsLevels.progress(5_000)).isEqualTo(new BedWarsLevels.Progress(2, 0, 5_000));
    }

    @Test
    void endOfMatchXpAddsMinutesTeammatesAndTheWin() {
        XpRewards rewards = new XpRewards(10, 5, 250, 100, 12, 60);

        assertThat(rewards.endOfMatch(3, 2, true)).isEqualTo(30 + 10 + 250);
        assertThat(rewards.endOfMatch(3, 2, false)).isEqualTo(40);
    }
}

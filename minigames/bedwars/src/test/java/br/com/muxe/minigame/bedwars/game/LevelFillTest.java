package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import org.junit.jupiter.api.Test;

class LevelFillTest {

    @Test
    void theXpBarShowsHowFarThePlayerIsInTheCurrentLevel() {
        BedWarsLevels.Progress halfway = BedWarsLevels.progress(2_500);
        BedWarsLevels.Progress nextLevel = BedWarsLevels.progress(5_000);

        assertThat(halfway.level()).isEqualTo(1);
        assertThat(halfway.fill()).isCloseTo(0.5f, within(1e-6f));
        assertThat(nextLevel.level()).isEqualTo(2);
        assertThat(nextLevel.fill()).isZero();
    }

    @Test
    void theBarNeverLooksFull() {
        assertThat(new BedWarsLevels.Progress(3, 5_000, 5_000).fill()).isEqualTo(BedWarsLevels.Progress.FULLEST_BAR);
        assertThat(new BedWarsLevels.Progress(3, 0, 0).fill()).isZero();
    }
}

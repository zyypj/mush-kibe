package br.com.muxe.minigame.bedwars.lobby.format;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.stats.LevelBadges;
import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import org.junit.jupiter.api.Test;

class LobbyFormatTest {

    private final LevelBadges badges = LevelBadges.from(RoomFixtures.CONFIG.getConfigurationSection("levels"));

    @Test
    void levelBadgesFollowTheLevelList() {
        assertThat(badges.of(0)).isEqualTo("&7[0&l\u272b&7]");
        assertThat(badges.of(9)).isEqualTo("&7[9&l\u272b&7]");
        assertThat(badges.of(10)).isEqualTo("&a[10&l\u2736&a]");
        assertThat(badges.of(126)).isEqualTo("&4[126&l\u2764&4]");
        assertThat(badges.of(484)).isEqualTo("&d[&5484\u2741&d]");
        assertThat(badges.of(537)).isEqualTo("&7[&f537\u274b&7]");
        assertThat(badges.of(669)).isEqualTo("&1[&9669\u271f&1]");
    }

    @Test
    void levelsFromAThousandAreRainbow() {
        assertThat(badges.of(1000)).isEqualTo("&c[&61&e0&a0&b0&d\u03a9&5]");
        assertThat(badges.of(1020)).isEqualTo("&c[&61&e0&a2&b0&d\u03a9&5]");
    }

    @Test
    void theXpBarShowsTheProgressOfTheCurrentLevel() {
        assertThat(XpBar.of(BedWarsLevels.progress(0), 10)).isEqualTo("&f[&a&7\u25ae\u25ae\u25ae\u25ae\u25ae\u25ae\u25ae\u25ae\u25ae\u25ae&f] &7(0/5k)");
        assertThat(XpBar.of(new BedWarsLevels.Progress(126, 31_800, 60_000), 12)).isEqualTo("&f[&a\u25ae\u25ae\u25ae\u25ae\u25ae\u25ae&7\u25ae\u25ae\u25ae\u25ae\u25ae\u25ae&f] &7(31.8k/60k)");
    }

    @Test
    void bigNumbersAreShortened() {
        assertThat(XpBar.compact(999)).isEqualTo("999");
        assertThat(XpBar.compact(5_000)).isEqualTo("5k");
        assertThat(XpBar.compact(31_850)).isEqualTo("31.8k");
    }
}

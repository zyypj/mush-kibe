package br.com.muxe.minigame.bedwars.tnt;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.minigame.bedwars.BedWarsPlugin;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import org.junit.jupiter.api.Test;

class TntTimerSettingsTest {

    private final TntTimerSettings settings = TntTimerSettings.from(RoomFixtures.CONFIG.getConfigurationSection("tnt-timer"));

    @Test
    void theTimerShowsTheSecondsLeftWithTwoDecimalsAndAComma() {
        assertThat(settings.text(59, 60)).isEqualTo("&22,95s");
        assertThat(settings.text(0, 60)).isEqualTo("&c0,00s");
    }

    @Test
    void englishReadersSeeTheSecondsWithADecimalPoint() {
        LocalizedConfig texts = LocalizedConfig.bundled(BedWarsPlugin.class, "config.yml");
        TntTimerSettings english = TntTimerSettings.from(texts.in(Language.ENGLISH).getConfigurationSection("tnt-timer"));
        TntTimerSettings portuguese = TntTimerSettings.from(texts.in(Language.PORTUGUESE).getConfigurationSection("tnt-timer"));

        assertThat(english.text(59, 60)).isEqualTo("&22.95s");
        assertThat(portuguese.text(59, 60)).isEqualTo("&22,95s");
        assertThat(english.text(17, 60)).startsWith(portuguese.text(17, 60).substring(0, 2));
    }

    @Test
    void theColourGoesFromDarkGreenToRedAsTheFuseBurns() {
        assertThat(settings.text(54, 60)).startsWith("&2");
        assertThat(settings.text(53, 60)).startsWith("&a");
        assertThat(settings.text(36, 60)).startsWith("&a");
        assertThat(settings.text(35, 60)).startsWith("&e");
        assertThat(settings.text(18, 60)).startsWith("&e");
        assertThat(settings.text(17, 60)).startsWith("&c");
    }

    @Test
    void itIsSeenWithinTheViewDistanceAndUpdatedEveryTick() {
        assertThat(settings.viewDistance()).isEqualTo(24);
        assertThat(settings.updateTicks()).isEqualTo(1);
    }
}

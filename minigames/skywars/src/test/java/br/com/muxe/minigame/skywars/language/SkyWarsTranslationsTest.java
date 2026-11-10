package br.com.muxe.minigame.skywars.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.minigame.skywars.SkyWarsPlugin;
import org.junit.jupiter.api.Test;

class SkyWarsTranslationsTest {

    private static final LocalizedConfig CONFIG = LocalizedConfig.bundled(SkyWarsPlugin.class, "config.yml");

    @Test
    void everyPlayerTextHasAnEnglishTranslation() {
        assertThat(CONFIG.untranslated(Language.ENGLISH, "")).isEmpty();
    }

    @Test
    void theModeNamesAreTranslatedAndTheirRulesStayTheSame() {
        assertThat(CONFIG.in(Language.ENGLISH).getString("modes.duo.display-name")).isEqualTo("SkyWars Doubles");
        assertThat(CONFIG.in(Language.PORTUGUESE).getString("modes.duo.display-name")).isEqualTo("SkyWars Duplas");
        assertThat(CONFIG.in(Language.ENGLISH).getInt("modes.duo.max-players")).isEqualTo(CONFIG.in(Language.PORTUGUESE).getInt("modes.duo.max-players"));
        assertThat(CONFIG.in(Language.ENGLISH).getInt("modes.duo.team-size")).isEqualTo(2);
    }
}

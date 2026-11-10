package br.com.muxe.lobby.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.lobby.MuxeLobbyPlugin;
import br.com.muxe.lobby.config.LobbySettings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LobbyTranslationsTest {

    private static final LocalizedConfig CONFIG = LocalizedConfig.bundled(MuxeLobbyPlugin.class, "config.yml");

    @Test
    void everyPlayerTextHasAnEnglishTranslation() {
        assertThat(CONFIG.untranslated(Language.ENGLISH, "")).isEmpty();
    }

    @Test
    void repliesFollowTheReadersLanguage() {
        UUID english = UUID.randomUUID();
        MessageCatalog messages = MessageCatalog.from(CONFIG, "messages").speaking(player -> player.equals(english) ? Language.ENGLISH : Language.PORTUGUESE);

        assertThat(messages.get(english, "play-unknown", "games", "skywars")).isEqualTo("§cUnknown mode. Modes: §fskywars");
        assertThat(messages.get(UUID.randomUUID(), "play-unknown", "games", "skywars")).isEqualTo("§cModo desconhecido. Modos: §fskywars");
        assertThat(messages.get(english, "fly-enabled")).isEqualTo("§eYou turned §bfly mode §eon!");
    }

    @Test
    void theEnglishMessagesKeepThePlaceholdersOfThePortugueseOnes() {
        for (Map.Entry<String, Object> entry : CONFIG.in(Language.PORTUGUESE).getConfigurationSection("messages").getValues(true).entrySet()) {
            if (entry.getValue() instanceof String portuguese) {
                String english = CONFIG.in(Language.ENGLISH).getString("messages." + entry.getKey());
                assertThat(placeholders(english)).as(entry.getKey()).isEqualTo(placeholders(portuguese));
            }
        }
    }

    @Test
    void theEnglishScoreboardKeepsTheShapeAndPlaceholdersOfThePortugueseOne() {
        List<String> portuguese = CONFIG.in(Language.PORTUGUESE).getStringList("scoreboard.lines");
        List<String> english = CONFIG.in(Language.ENGLISH).getStringList("scoreboard.lines");

        assertThat(english).hasSameSizeAs(portuguese);
        for (int line = 0; line < portuguese.size(); line++) {
            assertThat(placeholders(english.get(line))).as("line " + line).isEqualTo(placeholders(portuguese.get(line)));
            assertThat(english.get(line).isEmpty()).as("line " + line).isEqualTo(portuguese.get(line).isEmpty());
        }
    }

    @Test
    void settingsThatAreNotTextAreTheSameInEnglish() {
        LobbySettings portuguese = CONFIG.map(LobbySettings::from).in(Language.PORTUGUESE);
        LobbySettings english = CONFIG.map(LobbySettings::from).in(Language.ENGLISH);

        assertThat(english.games()).isEqualTo(portuguese.games());
        assertThat(english.scoreboardInterval()).isEqualTo(portuguese.scoreboardInterval());
        assertThat(english.bossBar().ticksPerFrame()).isEqualTo(portuguese.bossBar().ticksPerFrame());
        assertThat(english.launchPad()).isEqualTo(portuguese.launchPad());
    }

    private static Set<String> placeholders(String text) {
        Set<String> found = new LinkedHashSet<>();
        int start = text.indexOf('{');
        while (start >= 0) {
            int end = text.indexOf('}', start);
            if (end < 0) {
                break;
            }
            found.add(text.substring(start, end + 1));
            start = text.indexOf('{', end);
        }
        return found;
    }
}

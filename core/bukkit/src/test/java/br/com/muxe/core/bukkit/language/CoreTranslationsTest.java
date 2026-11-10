package br.com.muxe.core.bukkit.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoreTranslationsTest {

    private static final Set<String> STAFF_ONLY_PORTUGUESE_PATHS = Set.of();

    private final LocalizedConfig texts = LocalizedConfig.bundled(CoreTranslationsTest.class, "config.yml");

    @Test
    void everyReadableTextOfTheCoreConfigHasAnEnglishVersion() {
        Set<String> missing = new LinkedHashSet<>(texts.untranslated(Language.ENGLISH, ""));
        missing.removeAll(STAFF_ONLY_PORTUGUESE_PATHS);

        assertThat(missing).isEmpty();
    }

    @Test
    void theParkourHologramsAreTranslatedLineByLine() {
        Object start = texts.in(Language.ENGLISH).getMapList("parkour.holograms.start").get(1).get("text");
        Object height = texts.in(Language.ENGLISH).getMapList("parkour.holograms.start").get(1).get("height");

        assertThat(start).isEqualTo("&aStart");
        assertThat(height).isEqualTo(texts.in(Language.PORTUGUESE).getMapList("parkour.holograms.start").get(1).get("height"));
    }

    @Test
    void theEnglishMessagesKeepThePlaceholdersOfThePortugueseOnes() {
        MessageCatalog messages = MessageCatalog.from(texts, "messages");

        assertThat(messages.get(Language.ENGLISH, "command-usage", "usage", "/party")).isEqualTo("§cUsage: §f/party");
        assertThat(messages.get(Language.PORTUGUESE, "command-usage", "usage", "/party")).isEqualTo("§cUso: §f/party");
        for (Map.Entry<String, Object> entry : texts.in(Language.PORTUGUESE).getConfigurationSection("messages").getValues(true).entrySet()) {
            if (entry.getValue() instanceof String portuguese) {
                String english = texts.in(Language.ENGLISH).getString("messages." + entry.getKey());
                assertThat(placeholders(english)).as(entry.getKey()).isEqualTo(placeholders(portuguese));
            }
        }
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

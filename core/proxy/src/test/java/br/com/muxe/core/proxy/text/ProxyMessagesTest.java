package br.com.muxe.core.proxy.text;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.source.MapConfigSource;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.proxy.language.LocalizedConfig;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

class ProxyMessagesTest {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ProxyMessages messages = new ProxyMessages(new MapConfigSource(Map.of("messages", Map.of(
        "single", "&cno {name}",
        "screen", List.of("&cfirst {name}", "", "&7second")
    ))));

    private final UUID english = UUID.randomUUID();
    private final UUID portuguese = UUID.randomUUID();
    private final ProxyMessages localized = new ProxyMessages(
        LocalizedConfig.of(
            new MapConfigSource(Map.of("messages", Map.of(
                "greeting", "&aOlá {name}",
                "only-portuguese", "&cSó em português"
            ))),
            Map.of(Language.ENGLISH, new MapConfigSource(Map.of("messages", Map.of(
                "greeting", "&aHello {name}"
            ))))
        ),
        player -> player.equals(english) ? Language.ENGLISH : Language.PORTUGUESE
    );

    @Test
    void aSingleMessageKeepsItsColorsAndPlaceholders() {
        assertThat(LEGACY.serialize(messages.get("single", "name", "lobby"))).isEqualTo("§cno lobby");
    }

    @Test
    void aListBecomesOneMessageWithLineBreaks() {
        assertThat(LEGACY.serialize(messages.get("screen", "name", "lobby"))).isEqualTo("§cfirst lobby\n\n§7second");
    }

    @Test
    void aMissingMessageSaysWhichKeyIsMissing() {
        assertThat(LEGACY.serialize(messages.get("nothing"))).isEqualTo("§cMissing message: nothing");
    }

    @Test
    void eachViewerReadsTheMessageInTheirOwnLanguage() {
        assertThat(LEGACY.serialize(localized.get(english, "greeting", "name", "tadeu"))).isEqualTo("§aHello tadeu");
        assertThat(LEGACY.serialize(localized.get(portuguese, "greeting", "name", "tadeu"))).isEqualTo("§aOlá tadeu");
        assertThat(LEGACY.serialize(localized.get(Language.ENGLISH, "greeting", "name", "tadeu"))).isEqualTo("§aHello tadeu");
    }

    @Test
    void aMessageWithoutAnEnglishTextFallsBackToPortuguese() {
        assertThat(LEGACY.serialize(localized.get(english, "only-portuguese"))).isEqualTo("§cSó em português");
    }

    @Test
    void theViewerlessMessageIsPortuguese() {
        assertThat(LEGACY.serialize(localized.get("greeting", "name", "tadeu"))).isEqualTo("§aOlá tadeu");
    }
}

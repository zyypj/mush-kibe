package br.com.muxe.core.proxy.text;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.source.MapConfigSource;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

class ProxyMessagesTest {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ProxyMessages messages = new ProxyMessages(new MapConfigSource(Map.of("messages", Map.of(
        "single", "&cno {name}",
        "screen", List.of("&cfirst {name}", "", "&7second")
    ))));

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
}

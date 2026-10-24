package br.com.muxe.core.bukkit.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MessageCatalogTest {

    @Test
    void listsAreReadLineByLineWithColorsAndBlankLines() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n",
            "messages:",
            "  challenge:",
            "    - \"&6&lDESAFIO:\"",
            "    - \"\"",
            "    - \"&7hint\"",
            "  single: \"&aok\""
        ));

        MessageCatalog messages = MessageCatalog.from(config.getConfigurationSection("messages"));

        assertThat(messages.lines("challenge")).containsExactly("§6§lDESAFIO:", MessageCatalog.BLANK_LINE, "§7hint");
        assertThat(messages.lines("single")).containsExactly("§aok");
        assertThat(messages.get("single")).isEqualTo("§aok");
    }

    @Test
    void aListReadAsOneMessageIsJoinedByLineBreaks() {
        MessageCatalog messages = MessageCatalog.of(Map.of("kick", List.of("&cfirst", "&7second")));

        assertThat(messages.get("kick")).isEqualTo("§cfirst\n§7second");
    }

    @Test
    void placeholdersAreReplacedInEveryLine() {
        MessageCatalog messages = MessageCatalog.of(Map.of("info", List.of("spawn {spawn}", "code {code}")));

        assertThat(messages.lines("info", "spawn", "a", "code", "b")).containsExactly("spawn a", "code b");
    }

    @Test
    void aMissingListSaysWhichKeyIsMissing() {
        assertThat(MessageCatalog.of(Map.of()).lines("nothing")).containsExactly("§cMissing message: nothing");
    }

    @Test
    void theBlankLineShowsAsAnEmptyChatLine() {
        assertThat(MessageCatalog.BLANK_LINE).isNotEmpty();
        assertThat(MessageCatalog.BLANK_LINE.strip()).isEmpty();
    }

    @Test
    void mergedCatalogsKeepLists() {
        MessageCatalog merged = MessageCatalog.of(Map.of("a", "one")).merge(MessageCatalog.of(Map.of("b", List.of("x", "y"))));

        assertThat(merged.lines("b")).containsExactly("x", "y");
        assertThat(merged.get("a")).isEqualTo("one");
    }
}

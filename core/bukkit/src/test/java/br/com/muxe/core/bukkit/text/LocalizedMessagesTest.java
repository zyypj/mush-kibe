package br.com.muxe.core.bukkit.text;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Text;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LocalizedMessagesTest {

    private static final UUID ENGLISH_PLAYER = UUID.randomUUID();
    private static final UUID PORTUGUESE_PLAYER = UUID.randomUUID();
    private static final LanguageLookup LOOKUP = player -> player.equals(ENGLISH_PLAYER) ? Language.ENGLISH : Language.PORTUGUESE;

    @Test
    void eachPlayerReadsTheMessageInTheirLanguage() throws InvalidConfigurationException {
        MessageCatalog messages = MessageCatalog.from(config(), "messages").speaking(LOOKUP);

        assertThat(messages.get(ENGLISH_PLAYER, "greeting", "player", "tadeu")).isEqualTo("§aHello, tadeu!");
        assertThat(messages.get(PORTUGUESE_PLAYER, "greeting", "player", "tadeu")).isEqualTo("§aOlá, tadeu!");
        assertThat(messages.get("greeting", "player", "tadeu")).isEqualTo("§aOlá, tadeu!");
    }

    @Test
    void anUntranslatedMessageFallsBackToPortuguese() throws InvalidConfigurationException {
        MessageCatalog messages = MessageCatalog.from(config(), "messages").speaking(LOOKUP);

        assertThat(messages.get(ENGLISH_PLAYER, "only-portuguese")).isEqualTo("§cSó em português");
        assertThat(messages.lines(Language.ENGLISH, "rules")).containsExactly("§eRule one", MessageCatalog.BLANK_LINE, "§eRule two");
    }

    @Test
    void translatedPlaceholdersFollowTheReader() throws InvalidConfigurationException {
        MessageCatalog messages = MessageCatalog.from(config(), "messages").speaking(LOOKUP);
        Text team = Text.of("Vermelho", "Red");

        assertThat(messages.get(ENGLISH_PLAYER, "eliminated", "team", team)).isEqualTo("§cTeam Red was eliminated.");
        assertThat(messages.get(PORTUGUESE_PLAYER, "eliminated", "team", team)).isEqualTo("§cO time Vermelho foi eliminado.");
        assertThat(messages.text("eliminated", "team", team).in(Language.ENGLISH)).isEqualTo("§cTeam Red was eliminated.");
    }

    @Test
    void mergingKeepsTheLanguageOfThePlayers() throws InvalidConfigurationException {
        MessageCatalog core = MessageCatalog.from(config(), "messages").speaking(LOOKUP);
        MessageCatalog plugin = MessageCatalog.of(Map.of(Language.PORTUGUESE, Map.of("extra", "&aExtra"), Language.ENGLISH, Map.of("extra", "&aMore")), LanguageLookup.DEFAULT);

        MessageCatalog merged = core.merge(plugin);
        MessageCatalog overridden = plugin.merge(core);

        assertThat(merged.get(ENGLISH_PLAYER, "extra")).isEqualTo("§aMore");
        assertThat(overridden.get(ENGLISH_PLAYER, "greeting", "player", "x")).isEqualTo("§aHello, x!");
    }

    @Test
    void theEnglishConfigReportsWhatIsLeftToTranslate() throws InvalidConfigurationException {
        LocalizedConfig config = config();

        assertThat(config.untranslated(Language.ENGLISH, "messages")).containsExactly("messages.only-portuguese");
        assertThat(config.in(Language.ENGLISH).getString("menu.title")).isEqualTo("Shop");
        assertThat(config.in(Language.ENGLISH).getInt("menu.size")).isEqualTo(54);
        assertThat(config.in(Language.ENGLISH).getConfigurationSection("menu").getKeys(false)).contains("title", "size");
        assertThat(config.map(root -> root.getString("menu.title")).in(Language.PORTUGUESE)).isEqualTo("Loja");
    }

    private static LocalizedConfig config() throws InvalidConfigurationException {
        YamlConfiguration portuguese = new YamlConfiguration();
        portuguese.loadFromString(String.join("\n",
            "menu:",
            "  title: \"Loja\"",
            "  size: 54",
            "messages:",
            "  greeting: \"&aOlá, {player}!\"",
            "  only-portuguese: \"&cSó em português\"",
            "  eliminated: \"&cO time {team} foi eliminado.\"",
            "  separator: \"&7-----\"",
            "  rules:",
            "    - \"&eRegra um\"",
            "    - \"\"",
            "    - \"&eRegra dois\""
        ));
        YamlConfiguration english = new YamlConfiguration();
        english.loadFromString(String.join("\n",
            "menu:",
            "  title: \"Shop\"",
            "messages:",
            "  greeting: \"&aHello, {player}!\"",
            "  eliminated: \"&cTeam {team} was eliminated.\"",
            "  rules:",
            "    - \"&eRule one\"",
            "    - \"\"",
            "    - \"&eRule two\""
        ));
        return LocalizedConfig.of(portuguese, Map.of(Language.ENGLISH, english));
    }
}

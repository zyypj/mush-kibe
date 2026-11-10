package br.com.muxe.core.bukkit.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LocalizedConfigTest {

    @Test
    void anUntranslatedValueIsReadFromPortugueseEvenWhenTheCallerGivesAFallback() throws InvalidConfigurationException {
        LocalizedConfig config = config();

        ConfigurationSection english = config.in(Language.ENGLISH).getConfigurationSection("menu");

        assertThat(english.getString("material", "")).isEqualTo("BED");
        assertThat(english.getInt("slot", -1)).isEqualTo(10);
        assertThat(english.getStringList("lore")).containsExactly("Click to join!");
        assertThat(english.getString("title", "fallback")).isEqualTo("Select game");
        assertThat(english.getKeys(false)).containsExactly("title", "slot", "material", "lore");
    }

    @Test
    void eachLanguageMapsToItsOwnSettings() throws InvalidConfigurationException {
        Map<Language, String> titles = Map.of(
            Language.PORTUGUESE, config().map(root -> root.getString("menu.title", "")).in(Language.PORTUGUESE),
            Language.ENGLISH, config().map(root -> root.getString("menu.title", "")).in(Language.ENGLISH));

        assertThat(titles).containsEntry(Language.PORTUGUESE, "Selecionar jogo").containsEntry(Language.ENGLISH, "Select game");
    }

    private static LocalizedConfig config() throws InvalidConfigurationException {
        YamlConfiguration portuguese = new YamlConfiguration();
        portuguese.loadFromString("""
            menu:
              title: "Selecionar jogo"
              slot: 10
              material: BED
              lore:
                - "Clique para entrar!"
            """);
        YamlConfiguration english = new YamlConfiguration();
        english.loadFromString("""
            menu:
              title: "Select game"
              lore:
                - "Click to join!"
            """);
        return LocalizedConfig.of(portuguese, Map.of(Language.ENGLISH, english));
    }
}

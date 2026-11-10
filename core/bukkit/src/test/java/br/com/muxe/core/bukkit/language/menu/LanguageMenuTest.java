package br.com.muxe.core.bukkit.language.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.common.language.domain.Language;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LanguageMenuTest {

    private final LocalizedConfig texts = LocalizedConfig.bundled(LanguageMenuTest.class, "config.yml");
    private final LanguageMenu menu = new LanguageMenu(texts.map(section -> LanguageMenuSettings.from(section.getConfigurationSection("language-menu"))));

    @Test
    void theMenuListsEveryLanguageAndHighlightsTheCurrentOne() {
        Menu portuguese = menu.build(Language.PORTUGUESE, (player, language) -> {
        });

        assertThat(portuguese.title()).isEqualTo("Selecionar idioma");
        assertThat(portuguese.size()).isEqualTo(27);
        assertThat(portuguese.buttons()).containsOnlyKeys(10, 11);
        assertThat(portuguese.button(10).orElseThrow().icon().material()).isEqualTo(Material.BOOK);
        assertThat(portuguese.button(10).orElseThrow().icon().name()).isEqualTo("§aPortuguês");
        assertThat(portuguese.button(10).orElseThrow().icon().isGlowing()).isTrue();
        assertThat(portuguese.button(11).orElseThrow().icon().isGlowing()).isFalse();
    }

    @Test
    void eachLanguageIsDescribedInItsOwnLanguageWhileTheTitleFollowsTheReader() {
        Menu english = menu.build(Language.ENGLISH, (player, language) -> {
        });

        assertThat(english.title()).isEqualTo("Select language");
        assertThat(english.button(10).orElseThrow().icon().lore()).containsExactly("§7Alterar seu idioma para", "§7Português.", "", "§eClique para alterar seu idioma!");
        assertThat(english.button(11).orElseThrow().icon().lore()).containsExactly("§7Change your language", "§7to English.", "", "§eClick to change your language!");
        assertThat(english.button(11).orElseThrow().icon().isGlowing()).isTrue();
        assertThat(english.button(10).orElseThrow().icon().isGlowing()).isFalse();
    }

    @Test
    void aLanguageOutsideTheMenuIsRefused() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
            rows: 1
            languages:
              en:
                slot: 9
                item:
                  material: BOOK
            """);

        assertThatThrownBy(() -> LanguageMenuSettings.from(config)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anUnknownLanguageIsRefused() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
            languages:
              klingon:
                slot: 1
                item:
                  material: BOOK
            """);

        assertThatThrownBy(() -> LanguageMenuSettings.from(config)).isInstanceOf(IllegalArgumentException.class);
    }
}

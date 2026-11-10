package br.com.muxe.core.bukkit.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import java.util.List;
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
    void readingTheTranslationsLeavesTheServerFileAsItWas() throws InvalidConfigurationException {
        YamlConfiguration bundled = new YamlConfiguration();
        bundled.loadFromString("""
            title: "Jogo"
            room:
              games:
                bedwars:
                  material: BED
                  name: "&aBed Wars"
            """);
        YamlConfiguration server = new YamlConfiguration();
        server.set("title", "Meu jogo");
        server.setDefaults(bundled);
        YamlConfiguration english = new YamlConfiguration();
        english.loadFromString("""
            room:
              games:
                bedwars:
                  name: "&aBed Wars!"
            """);

        LocalizedConfig config = LocalizedConfig.of(server, Map.of(Language.ENGLISH, english));

        assertThat(server.getKeys(true)).containsExactly("title");
        assertThat(config.in(Language.ENGLISH).getString("room.games.bedwars.material", "")).isEqualTo("BED");
        assertThat(config.in(Language.ENGLISH).getString("room.games.bedwars.name", "")).isEqualTo("&aBed Wars!");
        assertThat(config.in(Language.ENGLISH).getString("title", "")).isEqualTo("Meu jogo");
    }

    @Test
    void eachLanguageMapsToItsOwnSettings() throws InvalidConfigurationException {
        Map<Language, String> titles = Map.of(
            Language.PORTUGUESE, config().map(root -> root.getString("menu.title", "")).in(Language.PORTUGUESE),
            Language.ENGLISH, config().map(root -> root.getString("menu.title", "")).in(Language.ENGLISH));

        assertThat(titles).containsEntry(Language.PORTUGUESE, "Selecionar jogo").containsEntry(Language.ENGLISH, "Select game");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aListOfItemsIsTranslatedItemByItemKeepingWhatTheEnglishLeavesOut() throws InvalidConfigurationException {
        YamlConfiguration portuguese = new YamlConfiguration();
        portuguese.loadFromString("""
            tiers:
              - name: "Picareta de Madeira"
                material: WOOD_PICKAXE
                price: 10
              - name: "Picareta de Ferro"
                material: IRON_PICKAXE
                price: 20
            tabs:
              - name: "Compra Rápida"
                slot: 0
            """);
        YamlConfiguration english = new YamlConfiguration();
        english.loadFromString("""
            tiers:
              - name: "Wooden Pickaxe"
              - name: "Iron Pickaxe"
            """);
        LocalizedConfig config = LocalizedConfig.of(portuguese, Map.of(Language.ENGLISH, english));

        List<Map<?, ?>> tiers = config.in(Language.ENGLISH).getMapList("tiers");

        assertThat(tiers).hasSize(2);
        assertThat((Map<Object, Object>) tiers.get(0)).containsEntry("name", "Wooden Pickaxe").containsEntry("material", "WOOD_PICKAXE").containsEntry("price", 10);
        assertThat((Map<Object, Object>) tiers.get(1)).containsEntry("name", "Iron Pickaxe").containsEntry("price", 20);
        assertThat((Map<Object, Object>) config.in(Language.PORTUGUESE).getMapList("tiers").get(0)).containsEntry("name", "Picareta de Madeira");
        assertThat(config.untranslated(Language.ENGLISH, "")).containsExactly("tabs");
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

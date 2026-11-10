package br.com.muxe.core.bukkit.customroom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CustomRoomLanguagesTest {

    @Test
    void aServerFileWithoutTheCustomRoomShowsTheGamesInEveryLanguage() {
        Localized<CustomRoomSettings> localized = localized();

        for (Language language : List.of(Language.ENGLISH, Language.PORTUGUESE, Language.ENGLISH, Language.PORTUGUESE)) {
            ConfigurationSection games = localized.in(language).section("selector").getConfigurationSection("games");
            assertThat(ItemBuilder.from(games.getConfigurationSection("bedwars")).material()).as(language.tag()).isEqualTo(Material.BED);
        }
    }

    private static Localized<CustomRoomSettings> localized() {
        YamlConfiguration server = new YamlConfiguration();
        server.set("hotbar.server-kinds", List.of("LOBBY"));
        server.setDefaults(ShopFixtures.CONFIG);
        LocalizedConfig texts = LocalizedConfig.of(server, Map.of(Language.ENGLISH, english()));
        return texts.map(root -> CustomRoomSettings.from(DefaultedSections.of(root, "custom-room")));
    }

    private static YamlConfiguration english() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
            CustomRoomLanguagesTest.class.getClassLoader().getResourceAsStream("lang/en.yml"), StandardCharsets.UTF_8));
    }
}

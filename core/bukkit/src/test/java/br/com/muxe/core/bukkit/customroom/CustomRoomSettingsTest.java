package br.com.muxe.core.bukkit.customroom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.combat.knockback.KnockbackProfiles;
import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CustomRoomSettingsTest {

    @Test
    void aServerFileWithoutTheCustomRoomStillShowsEveryGameAndMode() {
        YamlConfiguration server = serverFileWithoutNewSections();
        server.getConfigurationSection("custom-room");

        CustomRoomSettings settings = CustomRoomSettings.from(DefaultedSections.of(server, "custom-room"));

        assertThat(settings.section("selector.games").getKeys(false)).hasSize(12).contains("bedwars", "duels", "spleef");
        assertThat(settings.section("mode-menus.bedwars.modes").getKeys(false)).contains("solo", "duplas", "4s", "mega");
        assertThat(settings.item("room-menu.create").name()).isEqualTo("§aCriar sala");
        assertThat(settings.slot("room-menu.create")).isEqualTo(31);
    }

    @Test
    void aServerFileWithoutTheKnockbackStillHasTheProfiles() {
        YamlConfiguration server = serverFileWithoutNewSections();

        KnockbackProfiles profiles = KnockbackProfiles.from(DefaultedSections.of(server, "knockback"));

        assertThat(profiles.named("bedwars")).isPresent();
        assertThat(profiles.named("duels-combo")).isPresent();
    }

    private static YamlConfiguration serverFileWithoutNewSections() {
        YamlConfiguration server = new YamlConfiguration();
        server.set("hotbar.server-kinds", List.of("LOBBY"));
        server.setDefaults(ShopFixtures.CONFIG);
        return server;
    }
}

package br.com.muxe.core.bukkit.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DefaultedSectionsTest {

    @Test
    void aSectionMissingFromTheFileComesWholeFromTheDefaults() {
        YamlConfiguration file = fileWithDefaults();

        ConfigurationSection games = DefaultedSections.of(file, "custom-room.selector.games");

        assertThat(file.getConfigurationSection("custom-room.selector.games").getKeys(false)).isEmpty();
        assertThat(games.getKeys(false)).containsExactly("bedwars", "duels");
        assertThat(games.getInt("bedwars.slot")).isEqualTo(10);
    }

    @Test
    void aSectionInTheFileWins() {
        YamlConfiguration file = fileWithDefaults();
        file.set("custom-room.selector.games.skywars.slot", 12);

        ConfigurationSection games = DefaultedSections.of(file, "custom-room.selector.games");

        assertThat(games.getKeys(false)).containsExactly("skywars");
    }

    @Test
    void aSubSectionIsLookedUpUnderItsOwnPath() {
        YamlConfiguration file = fileWithDefaults();
        ConfigurationSection room = DefaultedSections.of(file, "custom-room");

        assertThat(DefaultedSections.of(room, "selector.games").getKeys(false)).containsExactly("bedwars", "duels");
    }

    @Test
    void anEmptySectionLeftInTheFileStillComesFromTheDefaults() {
        YamlConfiguration file = fileWithDefaults();
        file.createSection("custom-room.selector.games");

        assertThat(DefaultedSections.of(file, "custom-room.selector.games").getKeys(false)).containsExactly("bedwars", "duels");
    }

    @Test
    void aSectionNowhereIsEmpty() {
        assertThat(DefaultedSections.of(fileWithDefaults(), "nothing.here").getKeys(false)).isEmpty();
    }

    private static YamlConfiguration fileWithDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("custom-room.selector.games.bedwars.slot", 10);
        defaults.set("custom-room.selector.games.duels.slot", 11);
        YamlConfiguration file = new YamlConfiguration();
        file.set("hotbar.slot", 1);
        file.setDefaults(defaults);
        return file;
    }
}

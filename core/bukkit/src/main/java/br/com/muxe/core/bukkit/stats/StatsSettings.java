package br.com.muxe.core.bukkit.stats;

import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record StatsSettings(Hub hub, BedWars bedwars) {

    public static StatsSettings from(ConfigurationSection section) {
        return new StatsSettings(Hub.from(section), BedWars.from(section(section, "bedwars")));
    }

    public record Hub(String title, String titleOther, int rows, ConfiguredButton back, List<Game> games) {

        public Hub {
            games = List.copyOf(games);
        }

        static Hub from(ConfigurationSection section) {
            List<Game> games = new ArrayList<>();
            ConfigurationSection list = section(section, "games");
            for (String id : list.getKeys(false)) {
                ConfigurationSection game = section(list, id);
                games.add(new Game(ConfiguredButton.from(game), Opens.from(game.getString("opens", "nothing"))));
            }
            return new Hub(section.getString("title", ""), section.getString("title-other", ""), section.getInt("rows", 4),
                ConfiguredButton.from(section(section, "back")), games);
        }

        public String title(Target target) {
            return target.self() ? title : titleOther.replace("{name}", target.name());
        }
    }

    public record Game(ConfiguredButton button, Opens opens) {
    }

    public enum Opens {
        NOTHING,
        SOON,
        BEDWARS;

        static Opens from(String value) {
            return Opens.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
        }
    }

    public record BedWars(String title, String titleOther, int rows, ConfiguredButton back, ConfiguredButton general, List<String> lore, List<ConfiguredButton> modes,
                          LevelBadges badges) {

        public BedWars {
            lore = List.copyOf(lore);
            modes = List.copyOf(modes);
        }

        static BedWars from(ConfigurationSection section) {
            return new BedWars(section.getString("title", ""), section.getString("title-other", ""), section.getInt("rows", 6),
                ConfiguredButton.from(section(section, "back")), ConfiguredButton.from(section(section, "general")), section.getStringList("lore"),
                ConfiguredButton.list(section(section, "modes")), LevelBadges.from(section(section, "level-badges")));
        }

        public String title(Target target) {
            return target.self() ? title : titleOther.replace("{name}", target.name());
        }
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}

package br.com.muxe.minigame.bedwars.settings;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.timeline.Timeline.Entry;
import br.com.muxe.minigame.api.timeline.Timeline;
import br.com.muxe.minigame.bedwars.game.BedWarsEvent.Kind;
import br.com.muxe.minigame.bedwars.game.BedWarsEvent;
import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import br.com.muxe.minigame.bedwars.game.XpRewards;
import br.com.muxe.minigame.bedwars.item.ItemNames;
import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record BedWarsSettings(
    Timings timings,
    Forges forges,
    Timeline<BedWarsEvent> timeline,
    XpRewards xp,
    Holograms holograms,
    MatchSounds sounds,
    ItemNames items
) {

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};

    public static Localized<BedWarsSettings> from(LocalizedConfig texts) {
        Localized<ConfigurationSection> names = texts.map(BedWarsSettings::eventNames);
        return texts.map(root -> from(root, names));
    }

    public static BedWarsSettings from(ConfigurationSection config) {
        return from(config, Localized.same(eventNames(config)));
    }

    private static BedWarsSettings from(ConfigurationSection config, Localized<ConfigurationSection> names) {
        Forges forges = Forges.from(section(config, "forges"));
        return new BedWarsSettings(
            Timings.from(section(config, "timings")),
            forges,
            timeline(forges, section(config, "events"), names),
            XpRewards.from(section(config, "xp-rewards")),
            Holograms.from(section(config, "holograms")),
            MatchSounds.from(config.getConfigurationSection("sounds")),
            ItemNames.from(config.getConfigurationSection("item-names"))
        );
    }

    public static String roman(int tier) {
        return tier > 0 && tier < ROMAN.length ? ROMAN[tier] : String.valueOf(tier);
    }

    static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + path);
    }

    private static ConfigurationSection eventNames(ConfigurationSection config) {
        return section(section(config, "events"), "names");
    }

    private static Timeline<BedWarsEvent> timeline(Forges forges, ConfigurationSection events, Localized<ConfigurationSection> names) {
        List<Entry<BedWarsEvent>> list = new ArrayList<>();
        for (int tier = 2; tier <= forges.diamond().size(); tier++) {
            list.add(new Entry<>(new BedWarsEvent(Kind.DIAMOND_TIER, tier), forges.diamond().get(tier - 1).start(), name(names, "diamond", roman(tier))));
        }
        for (int tier = 2; tier <= forges.emerald().size(); tier++) {
            list.add(new Entry<>(new BedWarsEvent(Kind.EMERALD_TIER, tier), forges.emerald().get(tier - 1).start(), name(names, "emerald", roman(tier))));
        }
        list.add(new Entry<>(new BedWarsEvent(Kind.BEDS_DESTROYED, 0), events.getInt("beds-destroyed", 1800), name(names, "beds-destroyed", "")));
        list.add(new Entry<>(new BedWarsEvent(Kind.DRAGONS, 0), events.getInt("dragons", 2400), name(names, "dragons", "")));
        list.add(new Entry<>(new BedWarsEvent(Kind.GAME_END, 0), events.getInt("game-end", 2520), name(names, "game-end", "")));
        return new Timeline<>(list);
    }

    private static Text name(Localized<ConfigurationSection> names, String key, String tier) {
        return language -> names.in(language).getString(key, key).replace("{tier}", tier);
    }

    public record Timings(int islandRadius, int spawnRadius, int npcRadius, int generatorRadius) {

        static Timings from(ConfigurationSection section) {
            return new Timings(
                section.getInt("island-radius", 17),
                section.getInt("spawn-protection-radius", 5),
                section.getInt("npc-protection-radius", 1),
                section.getInt("generator-protection-radius", 1)
            );
        }
    }

    public record Tier(Rate rate, int start) {
    }

    public record Forges(boolean stackItems, double splitRadius, double ironDelay, int ironPerPlayer, int ironMax, int ironLimit,
                         Rate gold, Rate islandEmerald, List<Tier> diamond, List<Tier> emerald, Map<String, ModeIsland> modes) {

        public static final String FIVE_BY_FIVE = "5x5";

        public Forges {
            diamond = List.copyOf(diamond);
            emerald = List.copyOf(emerald);
            modes = Map.copyOf(modes);
        }

        static Forges from(ConfigurationSection section) {
            ConfigurationSection island = section(section, "island");
            ConfigurationSection iron = section(island, "iron");
            return new Forges(
                section.getBoolean("stack-items", false),
                section.getDouble("split-radius", 2),
                iron.getDouble("delay", 0.9),
                iron.getInt("amount-per-player", 1),
                iron.getInt("max-amount", 4),
                iron.getInt("spawn-limit", 48),
                rate(section(island, "gold")),
                rate(section(island, "emerald")),
                tiers(section(section, "diamond")),
                tiers(section(section, "emerald")),
                modes(section.getConfigurationSection("modes"))
            );
        }

        public Rate iron(int teamSize) {
            return new Rate(ironDelay, Math.max(1, Math.min(ironMax, ironPerPlayer * teamSize)), ironLimit);
        }

        public Rate iron(GameMode mode) {
            return island(mode.id()).map(ModeIsland::iron).orElseGet(() -> iron(mode.teamSize()));
        }

        public Rate gold(String mode) {
            return island(mode).map(ModeIsland::gold).orElse(gold);
        }

        public boolean fiveByFive(String mode) {
            return island(mode).map(ModeIsland::fiveByFive).orElse(false);
        }

        public Optional<ModeIsland> island(String mode) {
            return Optional.ofNullable(modes.get(mode));
        }

        private static Map<String, ModeIsland> modes(ConfigurationSection section) {
            Map<String, ModeIsland> modes = new LinkedHashMap<>();
            if (section == null) {
                return modes;
            }
            for (String mode : section.getKeys(false)) {
                ConfigurationSection island = section(section, mode);
                modes.put(mode, new ModeIsland(rate(section(island, "iron")), rate(section(island, "gold")),
                    FIVE_BY_FIVE.equals(island.getString("layout", ""))));
            }
            return modes;
        }

        private static Rate rate(ConfigurationSection section) {
            return new Rate(section.getDouble("delay"), section.getInt("amount", 1), section.getInt("spawn-limit", 0));
        }

        private static List<Tier> tiers(ConfigurationSection section) {
            List<Tier> tiers = new ArrayList<>();
            for (String key : List.of("tierI", "tierII", "tierIII")) {
                ConfigurationSection tier = section.getConfigurationSection(key);
                if (tier != null) {
                    tiers.add(new Tier(rate(tier), tier.getInt("start", 0)));
                }
            }
            if (tiers.isEmpty()) {
                throw new IllegalArgumentException("Generator " + section.getCurrentPath() + " has no tiers");
            }
            return tiers;
        }
    }

    public record ModeIsland(Rate iron, Rate gold, boolean fiveByFive) {
    }

    public record Holograms(List<String> shop, List<String> shopTeam, List<String> upgrades, List<String> upgradesTeam, List<String> diamond, List<String> emerald) {

        static Holograms from(ConfigurationSection section) {
            return new Holograms(
                section.getStringList("shop"),
                section.getStringList("shop-team"),
                section.getStringList("upgrades"),
                section.getStringList("upgrades-team"),
                section.getStringList("diamond"),
                section.getStringList("emerald")
            );
        }
    }
}

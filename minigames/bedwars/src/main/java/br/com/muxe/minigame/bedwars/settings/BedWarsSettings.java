package br.com.muxe.minigame.bedwars.settings;

import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import br.com.muxe.minigame.bedwars.game.Timeline;
import br.com.muxe.minigame.bedwars.game.Timeline.Event;
import br.com.muxe.minigame.bedwars.game.Timeline.Kind;
import br.com.muxe.minigame.bedwars.game.XpRewards;
import br.com.muxe.minigame.engine.match.CountdownTitles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record BedWarsSettings(
    String lobbyGroup,
    String defaultMode,
    Timings timings,
    CountdownTitles countdownTitles,
    Forges forges,
    Timeline timeline,
    XpRewards xp,
    ScoreboardSettings scoreboard,
    Holograms holograms,
    WaitingHotbar hotbar
) {

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};

    public static BedWarsSettings from(FileConfiguration config) {
        Forges forges = Forges.from(section(config, "forges"));
        return new BedWarsSettings(
            config.getString("lobby-group", "lobby"),
            config.getString("default-mode", "solo"),
            Timings.from(section(config, "timings")),
            CountdownTitles.from(config.getConfigurationSection("countdown.titles")),
            forges,
            timeline(forges, section(config, "events")),
            XpRewards.from(section(config, "xp-rewards")),
            ScoreboardSettings.from(section(config, "scoreboard")),
            Holograms.from(section(config, "holograms")),
            WaitingHotbar.from(section(config, "hotbar.waiting"))
        );
    }

    public static String roman(int tier) {
        return tier > 0 && tier < ROMAN.length ? ROMAN[tier] : String.valueOf(tier);
    }

    static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + path);
    }

    private static Timeline timeline(Forges forges, ConfigurationSection events) {
        ConfigurationSection names = section(events, "names");
        List<Event> list = new ArrayList<>();
        for (int tier = 2; tier <= forges.diamond().size(); tier++) {
            list.add(new Event(Kind.DIAMOND_TIER, tier, forges.diamond().get(tier - 1).start(), names.getString("diamond", "Diamante {tier}").replace("{tier}", roman(tier))));
        }
        for (int tier = 2; tier <= forges.emerald().size(); tier++) {
            list.add(new Event(Kind.EMERALD_TIER, tier, forges.emerald().get(tier - 1).start(), names.getString("emerald", "Esmeralda {tier}").replace("{tier}", roman(tier))));
        }
        list.add(new Event(Kind.BEDS_DESTROYED, 0, events.getInt("beds-destroyed", 1800), names.getString("beds-destroyed", "Camas destruídas")));
        list.add(new Event(Kind.DRAGONS, 0, events.getInt("dragons", 2400), names.getString("dragons", "Morte súbita")));
        list.add(new Event(Kind.GAME_END, 0, events.getInt("game-end", 2520), names.getString("game-end", "Fim de jogo")));
        return new Timeline(list);
    }

    public record Timings(int respawnSeconds, int rejoinSeconds, int restartSeconds, int respawnProtectionSeconds,
                          int islandRadius, int spawnRadius, int npcRadius, int generatorRadius) {

        static Timings from(ConfigurationSection section) {
            return new Timings(
                section.getInt("respawn-seconds", 5),
                section.getInt("rejoin-seconds", 180),
                section.getInt("restart-seconds", 30),
                section.getInt("respawn-protection-seconds", 4),
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
                         Rate gold, Rate islandEmerald, List<Tier> diamond, List<Tier> emerald) {

        public Forges {
            diamond = List.copyOf(diamond);
            emerald = List.copyOf(emerald);
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
                tiers(section(section, "emerald"))
            );
        }

        public Rate iron(int teamSize) {
            return new Rate(ironDelay, Math.max(1, Math.min(ironMax, ironPerPlayer * teamSize)), ironLimit);
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

    public record ScoreboardSettings(String title, List<String> waiting, List<String> starting, List<String> playing, List<String> playingEightTeams,
                                     String event, String noEvent, String team, String you, String online, String offline, String players, String eliminated) {

        static ScoreboardSettings from(ConfigurationSection section) {
            return new ScoreboardSettings(
                section.getString("title", "&b&lBED WARS"),
                section.getStringList("waiting"),
                section.getStringList("starting"),
                section.getStringList("playing"),
                section.getStringList("playing-eight-teams"),
                section.getString("event", "&f{event} em &a{time}"),
                section.getString("no-event", "&fFim de jogo"),
                section.getString("team", "{color}{letter} &f{name}: {status}{you}"),
                section.getString("team-you", " &7VOCÊ"),
                section.getString("status-online", "&a✔"),
                section.getString("status-offline", "&e✔"),
                section.getString("status-players", "&a{count}"),
                section.getString("status-eliminated", "&c✘")
            );
        }
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

    public record HotbarItem(int slot, String material, String name) {
    }

    public record WaitingHotbar(HotbarItem menu, HotbarItem leave) {

        static WaitingHotbar from(ConfigurationSection section) {
            return new WaitingHotbar(item(section(section, "menu")), item(section(section, "leave")));
        }

        private static HotbarItem item(ConfigurationSection section) {
            return new HotbarItem(section.getInt("slot"), section.getString("material", "STONE"), section.getString("name", ""));
        }
    }
}

package br.com.muxe.core.bukkit.mission.config;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.common.mission.domain.Mission;
import br.com.muxe.core.common.mission.domain.MissionCadence;
import br.com.muxe.core.common.mission.domain.MissionCatalog;
import br.com.muxe.core.common.mission.domain.MissionGoal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

public record MissionConfig(MissionCatalog catalog, Map<String, Texts> texts, Menu menu, Announce announce, Summary summary) {

    public MissionConfig {
        texts = Map.copyOf(texts);
    }

    public static MissionConfig empty() {
        return new MissionConfig(MissionCatalog.empty(), Map.of(), null, Announce.from(null), Summary.from(null));
    }

    public static MissionConfig from(ConfigurationSection section, Function<String, Optional<MissionGoal>> goals) {
        if (section == null) {
            return empty();
        }
        Map<String, Texts> texts = new HashMap<>();
        List<Mission> daily = missions(section.getConfigurationSection("daily"), MissionCadence.DAILY, texts, goals);
        List<Mission> weekly = missions(section.getConfigurationSection("weekly"), MissionCadence.WEEKLY, texts, goals);
        List<Mission> creditPath = missions(section.getConfigurationSection("credit-path"), MissionCadence.CREDIT_PATH, texts, goals);
        MissionCatalog catalog = new MissionCatalog(daily, weekly, creditPath, section.getInt("daily-count", 3), section.getInt("weekly-count", 5));
        return new MissionConfig(catalog, texts, Menu.from(Objects.requireNonNull(section.getConfigurationSection("menu"), "Missing missions.menu")),
            Announce.from(section.getConfigurationSection("announce")), Summary.from(section.getConfigurationSection("summary")));
    }

    private static List<Mission> missions(ConfigurationSection section, MissionCadence cadence, Map<String, Texts> texts,
                                          Function<String, Optional<MissionGoal>> goals) {
        List<Mission> missions = new ArrayList<>();
        if (section == null) {
            return missions;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection mission = Objects.requireNonNull(section.getConfigurationSection(id), "Missing mission " + id);
            String goalName = mission.getString("goal", "").toUpperCase(Locale.ROOT);
            MissionGoal goal = MissionGoal.WEEKLY_MISSIONS.id().equals(goalName) ? MissionGoal.WEEKLY_MISSIONS
                : goals.apply(goalName).orElseThrow(() -> new IllegalArgumentException("Unknown mission goal '" + goalName + "' at " + mission.getCurrentPath()));
            missions.add(new Mission(id, cadence, goal, mission.getLong("target", 1), mission.getLong("xp", 0), mission.getLong("credits", 0)));
            texts.put(id, new Texts(mission.getString("name", id), mission.getStringList("description")));
        }
        return missions;
    }

    public Texts texts(String mission) {
        return texts.getOrDefault(mission, new Texts(mission, List.of()));
    }

    public record Texts(String name, List<String> description) {

        public Texts {
            description = List.copyOf(description);
        }
    }

    public String label(MissionCadence cadence) {
        if (menu == null || !menu.categories().containsKey(cadence)) {
            return cadence.name();
        }
        return menu.categories().get(cadence).label();
    }

    public record Announce(String prefix, String name, String description, String progress, String xp, String credits) {

        static Announce from(ConfigurationSection section) {
            ConfigurationSection values = section == null ? new MemoryConfiguration() : section;
            return new Announce(
                values.getString("prefix", "&b&lMISSÕES &aVocê completou "),
                values.getString("name", "&e{name} &7({category})"),
                values.getString("description", "&7{description}"),
                values.getString("progress", "&f{progress}/{target}"),
                values.getString("xp", "&b+{xp} XP (Bed Wars)"),
                values.getString("credits", "&d+{credits} créditos")
            );
        }
    }

    public record Summary(String border, Map<MissionCadence, String> headers, String line, String completed) {

        public Summary {
            headers = Map.copyOf(headers);
        }

        static Summary from(ConfigurationSection section) {
            ConfigurationSection values = section == null ? new MemoryConfiguration() : section;
            Map<MissionCadence, String> headers = new EnumMap<>(MissionCadence.class);
            ConfigurationSection configured = values.getConfigurationSection("headers");
            for (MissionCadence cadence : MissionCadence.values()) {
                headers.put(cadence, configured == null ? cadence.name() : configured.getString(cadence.name(), cadence.name()));
            }
            return new Summary(
                values.getString("border", "&a----------------------------------------"),
                headers,
                values.getString("line", " &e{name}: &7{progress}/{target} &a(+{added})"),
                values.getString("completed", " &bCOMPLETO!")
            );
        }
    }


    public record Category(int slot, ItemBuilder item, String label) {
    }

    public record Menu(
        String title,
        int rows,
        int rowsWithBack,
        Map<MissionCadence, Category> categories,
        List<String> categoryLore,
        ItemBuilder mission,
        String completedName,
        String rewardOne,
        String rewardMany,
        String xpReward,
        String creditsReward,
        String inProgress,
        String completed,
        String progress,
        int backSlot,
        ItemBuilder back,
        Map<String, String> time
    ) {

        public Menu {
            categories = Map.copyOf(categories);
            categoryLore = List.copyOf(categoryLore);
            time = Map.copyOf(time);
        }

        static Menu from(ConfigurationSection section) {
            Map<MissionCadence, Category> categories = new EnumMap<>(MissionCadence.class);
            ConfigurationSection configured = Objects.requireNonNull(section.getConfigurationSection("categories"), "Missing missions.menu.categories");
            for (MissionCadence cadence : MissionCadence.values()) {
                ConfigurationSection category = configured.getConfigurationSection(cadence.name());
                if (category != null) {
                    categories.put(cadence, new Category(category.getInt("slot"), ItemBuilder.from(Objects.requireNonNull(category.getConfigurationSection("item"))),
                        category.getString("label", cadence.name())));
                }
            }
            ConfigurationSection back = Objects.requireNonNull(section.getConfigurationSection("back"), "Missing missions.menu.back");
            Map<String, String> time = new HashMap<>();
            ConfigurationSection times = section.getConfigurationSection("time");
            if (times != null) {
                times.getKeys(false).forEach(key -> time.put(key, times.getString(key)));
            }
            return new Menu(
                section.getString("title", "Missões do Bed Wars"),
                section.getInt("rows", 5),
                section.getInt("rows-with-back", 6),
                categories,
                section.getStringList("category-lore"),
                ItemBuilder.from(Objects.requireNonNull(section.getConfigurationSection("mission"), "Missing missions.menu.mission")),
                section.getString("completed-name", "&a{name}"),
                section.getString("reward-one", "&eRecompensa:"),
                section.getString("reward-many", "&eRecompensas:"),
                section.getString("xp-reward", "&b+{xp} XP (Bed Wars)"),
                section.getString("credits-reward", "&d+{credits} créditos"),
                section.getString("in-progress", "&eEm progresso."),
                section.getString("completed", "&aCompleta!"),
                section.getString("progress", "&a{progress}/{target}"),
                back.getInt("slot", 48),
                ItemBuilder.from(Objects.requireNonNull(back.getConfigurationSection("item"))),
                time
            );
        }
    }
}

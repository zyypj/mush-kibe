package br.com.muxe.core.bukkit.ultraplus;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.bukkit.text.DurationWords;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record UltraPlusSettings(String prefix, String example, long rewardCredits, ZoneId zone, Texts texts, DurationWords words, MainMenu main, OptionMenu colors,
                                EmblemMenu emblems) {

    private static final String COLOR_MARK = "&";

    public UltraPlusSettings {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(example, "example");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(texts, "texts");
        Objects.requireNonNull(words, "words");
        Objects.requireNonNull(main, "main");
        Objects.requireNonNull(colors, "colors");
        Objects.requireNonNull(emblems, "emblems");
    }

    public static UltraPlusSettings from(ConfigurationSection root) {
        ConfigurationSection main = section(root, "menus.ultra-plus");
        return new UltraPlusSettings(
            main.getString("prefix", "{color}&lULTRA{plus}+ "),
            main.getString("example", "{prefix}{color}{player}{emblem}"),
            main.getLong("reward.credits", 450),
            ZoneId.of(main.getString("reward.time-zone", "America/Sao_Paulo")),
            Texts.from(section(main, "texts")),
            DurationWords.from(root),
            MainMenu.from(main),
            OptionMenu.from(section(root, "menus.plus-colors")),
            EmblemMenu.from(section(root, "menus.emblems"))
        );
    }

    public String prefix(Option level, Option plus) {
        return prefix.replace("{color}", level.color()).replace("{plus}", plus.color());
    }

    public String example(Option level, Option plus, String emblem, String player) {
        return example.replace("{prefix}", prefix(level, plus)).replace("{color}", level.color()).replace("{player}", player).replace("{emblem}", emblem);
    }

    static String code(String color) {
        return color.startsWith(COLOR_MARK) ? color : COLOR_MARK + color;
    }

    static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }

    public record Texts(String included, String required, String progress, String exampleTitle, String selected, String emblemSelected, String select, String locked,
                        String exclusive, String resets, String claimed, String claim) {

        static Texts from(ConfigurationSection section) {
            return new Texts(section.getString("included", "&7Incluso no Ultra+"), section.getString("required", "&7Meses necessários:"),
                section.getString("progress", "&a{months}/{required} &7({percent}%)"), section.getString("example", "&7Exemplo:"),
                section.getString("selected", "&aSelecionada."), section.getString("emblem-selected", "&aSelecionado."),
                section.getString("select", "&eClique para selecionar."), section.getString("locked", "&cBloqueado."),
                section.getString("exclusive", "&7Exclusivo para &d&lULTRA&6+"), section.getString("resets", "&bReinicia em {time}."),
                section.getString("claimed", "&cJá resgatado."), section.getString("claim", "&eClique para resgatar."));
        }
    }

    public record Option(String id, int slot, String color, int months, ItemBuilder item) {

        public Option {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(item, "item");
        }

        static List<Option> list(ConfigurationSection section) {
            List<Option> options = new ArrayList<>();
            for (String id : section.getKeys(false)) {
                ConfigurationSection option = section(section, id);
                options.add(new Option(id, option.getInt("slot"), code(option.getString("color", "f")), option.getInt("months", 0), ItemBuilder.from(section(option, "item"))));
            }
            return List.copyOf(options);
        }
    }

    public record Emblem(String id, int slot, String symbol, Optional<String> color, ItemBuilder item) {

        public Emblem {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(symbol, "symbol");
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(item, "item");
        }

        public String render() {
            return color.orElse("") + symbol;
        }
    }

    public record MainMenu(String title, int rows, ConfiguredButton back, int rewardSlot, ItemBuilder reward, ConfiguredButton colors, List<Option> levels,
                           ItemBuilder pane, int dashes) {

        public MainMenu {
            levels = List.copyOf(levels);
            if (levels.isEmpty()) {
                throw new IllegalArgumentException("The Ultra+ menu needs at least one tag level");
            }
        }

        static MainMenu from(ConfigurationSection section) {
            ConfigurationSection reward = section(section, "reward");
            return new MainMenu(section.getString("title", "Menu do Ultra+"), section.getInt("rows", 5), ConfiguredButton.from(section(section, "back")),
                reward.getInt("slot"), ItemBuilder.from(section(reward, "item")), ConfiguredButton.from(section(section, "plus-color")),
                Option.list(section(section, "levels")), ItemBuilder.from(section(section, "progress")), section.getInt("progress.dashes", 10));
        }

        public Option level(int number) {
            return levels.stream().filter(level -> level.id().equals(Integer.toString(number))).findFirst().orElse(levels.getFirst());
        }
    }

    public record OptionMenu(String title, int rows, ConfiguredButton back, List<Option> options) {

        public OptionMenu {
            options = List.copyOf(options);
            if (options.isEmpty()) {
                throw new IllegalArgumentException("The plus colour menu needs at least one colour");
            }
        }

        static OptionMenu from(ConfigurationSection section) {
            return new OptionMenu(section.getString("title", ""), section.getInt("rows", 4), ConfiguredButton.from(section(section, "back")),
                Option.list(section(section, "colors")));
        }

        public Option option(Optional<String> id) {
            return id.flatMap(wanted -> options.stream().filter(option -> option.id().equals(wanted)).findFirst()).orElse(options.getFirst());
        }
    }

    public record EmblemMenu(String title, int rows, ConfiguredButton back, int noneSlot, ItemBuilder none, List<Emblem> emblems) {

        public EmblemMenu {
            emblems = List.copyOf(emblems);
        }

        static EmblemMenu from(ConfigurationSection section) {
            ConfigurationSection none = section(section, "none");
            List<Emblem> emblems = new ArrayList<>();
            ConfigurationSection list = section(section, "emblems");
            for (String id : list.getKeys(false)) {
                ConfigurationSection emblem = section(list, id);
                emblems.add(new Emblem(id, emblem.getInt("slot"), emblem.getString("symbol", ""), Optional.ofNullable(emblem.getString("color")).map(UltraPlusSettings::code),
                    ItemBuilder.from(section(emblem, "item"))));
            }
            return new EmblemMenu(section.getString("title", "Emblemas"), section.getInt("rows", 4), ConfiguredButton.from(section(section, "back")),
                none.getInt("slot"), ItemBuilder.from(section(none, "item")), emblems);
        }

        public Optional<Emblem> emblem(Optional<String> id) {
            return id.flatMap(wanted -> emblems.stream().filter(emblem -> emblem.id().equals(wanted)).findFirst());
        }
    }
}

package br.com.muxe.minigame.bedwars.preferences.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.cosmetic.menu.MenuSlot;
import br.com.muxe.minigame.bedwars.preferences.PlayerPreferences;
import br.com.muxe.minigame.bedwars.preferences.domain.Preferences.ChestDeposit;
import br.com.muxe.minigame.bedwars.preferences.domain.Preferences;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class PreferencesMenu {

    private final Localized<Settings> settings;
    private final LanguageLookup languages;
    private final PlayerPreferences preferences;

    public PreferencesMenu(Localized<Settings> settings, LanguageLookup languages, PlayerPreferences preferences) {
        this.settings = settings;
        this.languages = languages;
        this.preferences = preferences;
    }

    public void open(Player player, Consumer<Player> back) {
        UUID id = player.getUniqueId();
        Settings settings = this.settings.in(languages.of(id));
        Preferences current = preferences.of(id);
        Menu.Builder menu = Menu.builder(settings.title()).rows(settings.rows());
        toggle(menu, settings, settings.showLevel(), current.showLevel(), back, Preferences::toggleShowLevel);
        toggle(menu, settings, settings.tntTimer(), current.tntTimer(), back, Preferences::toggleTntTimer);
        Option chest = settings.chestDeposit();
        List<String> chestLore = chestLore(chest.description(), current.chestDeposit(), settings);
        ItemBuilder dye = ItemBuilder.of(Material.INK_SACK, settings.depositOptions().get(current.chestDeposit()).data());
        menu.button(chest.slot(), ItemBuilder.of(chest.material()).name(chest.name()).lore(chestLore),
            context -> change(context.player(), back, Preferences::nextChestDeposit));
        menu.button(chest.statusSlot(), dye.name(chest.name()).lore(chestLore), context -> change(context.player(), back, Preferences::nextChestDeposit));
        menu.button(settings.back().slot(), settings.back().item(), context -> back.accept(context.player()));
        menu.build().open(player);
    }

    private void toggle(Menu.Builder menu, Settings settings, Option option, boolean on, Consumer<Player> back, UnaryOperator<Preferences> change) {
        List<String> lore = toggleLore(option.description(), on, settings);
        ItemBuilder status = on ? settings.on() : settings.off();
        menu.button(option.slot(), ItemBuilder.of(option.material()).name(option.name()).lore(lore), context -> change(context.player(), back, change));
        menu.button(option.statusSlot(), status.name(option.name()).lore(lore), context -> change(context.player(), back, change));
    }

    private void change(Player player, Consumer<Player> back, UnaryOperator<Preferences> change) {
        UUID id = player.getUniqueId();
        preferences.change(id, change.apply(preferences.of(id)));
        open(player, back);
    }

    public static List<String> toggleLore(List<String> description, boolean on, Settings settings) {
        List<String> lore = new ArrayList<>(description);
        lore.add(" ");
        lore.add(on ? settings.valueOn() : settings.valueOff());
        lore.add(" ");
        lore.add(on ? settings.clickOff() : settings.clickOn());
        return lore;
    }

    public static List<String> chestLore(List<String> description, ChestDeposit selected, Settings settings) {
        List<String> lore = new ArrayList<>(description);
        lore.add(" ");
        for (ChestDeposit option : ChestDeposit.values()) {
            String name = settings.depositOptions().get(option).name();
            lore.add((option == selected ? settings.selected() : settings.option()).replace("{option}", name));
        }
        lore.add(" ");
        lore.add(settings.switchTo().replace("{option}", settings.depositOptions().get(selected.next()).name()));
        return lore;
    }

    public record Option(int slot, int statusSlot, Material material, String name, List<String> description) {

        public Option {
            description = List.copyOf(description);
        }

        static Option from(ConfigurationSection section) {
            return new Option(section.getInt("slot"), section.getInt("status-slot"),
                Objects.requireNonNull(Material.matchMaterial(section.getString("material", "STONE")), "Unknown material at " + section.getCurrentPath()),
                section.getString("name"), section.getStringList("description"));
        }
    }

    public record DepositOption(String name, int data) {
    }

    public record Settings(String title, int rows, String valueOn, String valueOff, String clickOff, String clickOn, ItemBuilder on, ItemBuilder off,
                           Option showLevel, Option tntTimer, Option chestDeposit, String option, String selected, String switchTo,
                           Map<ChestDeposit, DepositOption> depositOptions, MenuSlot back) {

        public Settings {
            depositOptions = Map.copyOf(depositOptions);
        }

        public static Settings from(ConfigurationSection section) {
            ConfigurationSection chest = section(section, "chest-deposit");
            ConfigurationSection options = section(chest, "options");
            Map<ChestDeposit, DepositOption> depositOptions = new EnumMap<>(ChestDeposit.class);
            for (ChestDeposit deposit : ChestDeposit.values()) {
                ConfigurationSection option = section(options, deposit.name().toLowerCase(Locale.ROOT).replace('_', '-'));
                depositOptions.put(deposit, new DepositOption(option.getString("name"), option.getInt("data")));
            }
            return new Settings(
                section.getString("title", ""),
                section.getInt("rows", 4),
                section.getString("value-on"),
                section.getString("value-off"),
                section.getString("click-off"),
                section.getString("click-on"),
                ItemBuilder.from(section(section, "status-on")),
                ItemBuilder.from(section(section, "status-off")),
                Option.from(section(section, "show-level")),
                Option.from(section(section, "tnt-timer")),
                Option.from(chest),
                chest.getString("option"),
                chest.getString("selected"),
                chest.getString("click"),
                depositOptions,
                MenuSlot.from(section(section, "back"))
            );
        }

        private static ConfigurationSection section(ConfigurationSection parent, String path) {
            return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
        }
    }
}

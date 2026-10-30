package br.com.muxe.minigame.bedwars.cosmetic.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record CosmeticMenuSettings(
    String bedwarsTitle,
    int bedwarsRows,
    MenuSlot cosmetics,
    String categoriesTitle,
    int categoriesRows,
    MenuSlot bedDestruction,
    Map<Integer, ItemBuilder> soon,
    MenuSlot categoriesBack,
    String listTitle,
    int listRows,
    List<Integer> listSlots,
    ItemBuilder none,
    ItemBuilder random,
    MenuSlot listBack,
    MenuSlot previous,
    MenuSlot next,
    MenuSlot selected,
    MenuSlot shop,
    LoreTexts lore
) {

    public CosmeticMenuSettings {
        soon = Map.copyOf(soon);
        listSlots = List.copyOf(listSlots);
    }

    public static CosmeticMenuSettings from(ConfigurationSection menus) {
        ConfigurationSection bedwars = section(menus, "bedwars");
        ConfigurationSection categories = section(menus, "categories");
        ConfigurationSection list = section(menus, "bed-destruction");
        Map<Integer, ItemBuilder> soon = new LinkedHashMap<>();
        ConfigurationSection soonSection = section(categories, "soon");
        for (String slot : soonSection.getKeys(false)) {
            soon.put(Integer.parseInt(slot), ItemBuilder.from(section(soonSection, slot)));
        }
        return new CosmeticMenuSettings(
            bedwars.getString("title", "Menu do Bed Wars"),
            bedwars.getInt("rows", 3),
            MenuSlot.from(section(bedwars, "cosmetics")),
            categories.getString("title", "Cosméticos do Bed Wars"),
            categories.getInt("rows", 6),
            MenuSlot.from(section(categories, "bed-destruction")),
            soon,
            MenuSlot.from(section(categories, "back")),
            list.getString("title", "Destruição de Cama ({page}/{pages})"),
            list.getInt("rows", 6),
            list.getIntegerList("slots"),
            ItemBuilder.from(section(list, "none")),
            ItemBuilder.from(section(list, "random")),
            MenuSlot.from(section(list, "back")),
            MenuSlot.from(section(list, "previous")),
            MenuSlot.from(section(list, "next")),
            MenuSlot.from(section(list, "selected")),
            MenuSlot.from(section(list, "shop")),
            LoreTexts.from(section(list, "lore"))
        );
    }

    static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }

    public record LoreTexts(
        String star,
        String starsOn,
        String starsOff,
        String ownedName,
        String lockedName,
        String ultraTag,
        List<String> selectOnly,
        List<String> select,
        List<String> selected,
        List<String> selectedWithPreview,
        List<String> buy,
        List<String> buyAffordable,
        List<String> unavailable
    ) {

        static LoreTexts from(ConfigurationSection section) {
            return new LoreTexts(
                section.getString("star", "*"),
                section.getString("stars-on", "&6&l"),
                section.getString("stars-off", "&7&l"),
                section.getString("owned-name", "&a{name}"),
                section.getString("locked-name", "&c{name}"),
                section.getString("ultra-tag", "&7Incluso no &d&lULTRA"),
                section.getStringList("select-only"),
                section.getStringList("select"),
                section.getStringList("selected"),
                section.getStringList("selected-with-preview"),
                section.getStringList("buy"),
                section.getStringList("buy-affordable"),
                section.getStringList("unavailable")
            );
        }
    }
}

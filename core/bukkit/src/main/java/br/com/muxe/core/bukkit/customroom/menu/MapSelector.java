package br.com.muxe.core.bukkit.customroom.menu;

import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket.Arena;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class MapSelector {

    private final CustomRoomSettings settings;

    public MapSelector(CustomRoomSettings settings) {
        this.settings = settings;
    }

    public static List<String> toggle(List<String> selected, String map, boolean shift) {
        List<String> changed = new ArrayList<>(selected);
        if (shift) {
            if (!changed.remove(map)) {
                changed.add(map);
            }
            return List.copyOf(changed);
        }
        if (changed.size() == 1 && changed.contains(map)) {
            return List.of();
        }
        return List.of(map);
    }

    public static String summary(List<String> selected, List<Arena> arenas, String none) {
        if (selected.isEmpty()) {
            return none;
        }
        List<String> names = new ArrayList<>();
        for (String map : selected) {
            names.add(arenas.stream().filter(arena -> arena.name().equals(map)).map(Arena::displayName).findFirst().orElse(map));
        }
        return String.join(", ", names);
    }

    public void open(Player player, Selection selection, int page) {
        ConfigurationSection section = settings.section("maps-menu");
        List<Integer> slots = section.getIntegerList("slots");
        List<Arena> arenas = selection.arenas().stream().sorted(Comparator.comparing(arena -> arena.displayName().toLowerCase(Locale.ROOT))).toList();
        int perPage = Math.max(1, slots.size());
        int pages = Math.max(1, (arenas.size() + perPage - 1) / perPage);
        int current = Math.max(1, Math.min(pages, page));
        String title = section.getString("title", "Selecione mapas ({page}/{pages})").replace("{page}", String.valueOf(current)).replace("{pages}", String.valueOf(pages));
        Menu.Builder menu = Menu.builder(Colors.translate(title)).rows(section.getInt("rows", 6));
        List<String> lore = section.getStringList("lore").stream().map(Colors::translate).toList();
        List<Arena> shown = arenas.subList((current - 1) * perPage, Math.min(arenas.size(), current * perPage));
        for (int index = 0; index < shown.size(); index++) {
            Arena arena = shown.get(index);
            boolean selected = selection.selected().contains(arena.name());
            Material material = Material.matchMaterial(section.getString(selected ? "selected-material" : "other-material", "MAP"));
            String name = section.getString(selected ? "selected-name" : "other-name", "{map}").replace("{map}", arena.displayName());
            menu.button(slots.get(index), ItemBuilder.of(material == null ? Material.MAP : material).name(Colors.translate(name)).lore(lore), context -> {
                List<String> changed = toggle(selection.selected(), arena.name(), context.isShiftClick());
                selection.changed().accept(changed);
                open(context.player(), selection.with(changed), current);
            });
        }
        if (current > 1) {
            menu.button(section.getInt("previous-slot", 48), pageButton(current - 1), context -> open(context.player(), selection, current - 1));
        } else {
            menu.button(section.getInt("back-slot", 48), OptionMenus.backButton(settings.texts(), selection.backTitle()), context -> selection.back().accept(context.player()));
        }
        if (current < pages) {
            menu.button(section.getInt("next-slot", 50), pageButton(current + 1), context -> open(context.player(), selection, current + 1));
        }
        String none = settings.text("room-menu.maps.none", "Nenhum");
        menu.button(settings.slot("maps-menu.summary"), settings.item("maps-menu.summary")
            .replacing("maps", summary(selection.selected(), arenas, none)));
        menu.button(settings.slot("maps-menu.external"), settings.item("maps-menu.external"));
        menu.build().open(player);
    }

    private ItemBuilder pageButton(int number) {
        return ItemBuilder.of(Material.ARROW).name(Colors.translate(settings.texts().get("page-name", "page", String.valueOf(number))));
    }

    public record Selection(List<Arena> arenas, List<String> selected, Consumer<List<String>> changed, Consumer<Player> back, String backTitle) {

        public Selection {
            arenas = List.copyOf(arenas);
            selected = List.copyOf(selected);
        }

        public Selection with(List<String> changedSelection) {
            return new Selection(arenas, changedSelection, changed, back, backTitle);
        }
    }
}

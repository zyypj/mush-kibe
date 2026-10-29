package br.com.muxe.core.bukkit.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class PaginatedMenu {

    private final String titleTemplate;
    private final List<MenuButton> items;
    private final List<Integer> fixedSlots;
    private final int fixedRows;
    private final ItemBuilder previous;
    private final ItemBuilder next;
    private final Map<Integer, MenuButton> extras;
    private final ItemBuilder fill;

    private PaginatedMenu(Builder builder) {
        this.titleTemplate = builder.title;
        this.items = List.copyOf(builder.items);
        this.fixedSlots = builder.slots == null ? null : List.copyOf(builder.slots);
        this.fixedRows = builder.rows;
        this.previous = builder.previous;
        this.next = builder.next;
        this.extras = Map.copyOf(builder.extras);
        this.fill = builder.fill;
    }

    public static Builder builder(String titleTemplate) {
        return new Builder(titleTemplate);
    }

    public int perPage() {
        return fixedSlots == null ? MenuLayout.ITEMS_PER_PAGE : fixedSlots.size();
    }

    public int pages() {
        return Page.of(items, perPage(), 1).total();
    }

    public Menu page(int requested) {
        Page<MenuButton> page = Page.of(items, perPage(), requested);
        int rows = rows();
        List<Integer> slots = fixedSlots == null ? MenuLayout.centered(page.items().size(), 1) : fixedSlots;
        Menu.Builder menu = Menu.builder(titleTemplate.replace("{page}", Integer.toString(page.number())).replace("{pages}", Integer.toString(page.total()))).rows(rows);
        extras.forEach(menu::button);
        for (int index = 0; index < page.items().size(); index++) {
            menu.button(slots.get(index), page.items().get(index));
        }
        int lastRow = rows - 1;
        if (page.hasPrevious()) {
            menu.button(Slots.of(lastRow, 0), previous, context -> context.open(page(page.number() - 1)));
        }
        if (page.hasNext()) {
            menu.button(Slots.of(lastRow, MenuLayout.ROW_WIDTH - 1), next, context -> context.open(page(page.number() + 1)));
        }
        if (fill != null) {
            menu.fill(fill);
        }
        return menu.build();
    }

    public MenuHolder open(Player player, int page) {
        return page(page).open(player);
    }

    private int rows() {
        if (fixedRows > 0) {
            return fixedRows;
        }
        int needed = MenuLayout.rowsFor(Math.min(items.size(), perPage()));
        return Math.min(Menu.MAX_ROWS, needed);
    }

    public static final class Builder {

        private final String title;
        private final List<MenuButton> items = new ArrayList<>();
        private List<Integer> slots;
        private int rows;
        private ItemBuilder previous = ItemBuilder.of(Material.ARROW).name("&aPágina anterior");
        private ItemBuilder next = ItemBuilder.of(Material.ARROW).name("&aPróxima página");
        private final Map<Integer, MenuButton> extras = new LinkedHashMap<>();
        private ItemBuilder fill;

        private Builder(String title) {
            this.title = title;
        }

        public Builder items(List<MenuButton> values) {
            items.addAll(values);
            return this;
        }

        public Builder item(MenuButton value) {
            items.add(value);
            return this;
        }

        public Builder slots(List<Integer> values) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("A paginated menu needs at least one slot");
            }
            this.slots = values;
            return this;
        }

        public Builder rows(int value) {
            if (value < 1 || value > Menu.MAX_ROWS) {
                throw new IllegalArgumentException("Menus have 1 to 6 rows, got " + value);
            }
            this.rows = value;
            return this;
        }

        public Builder previous(ItemBuilder icon) {
            this.previous = icon;
            return this;
        }

        public Builder next(ItemBuilder icon) {
            this.next = icon;
            return this;
        }

        public Builder extra(int slot, MenuButton button) {
            extras.put(slot, button);
            return this;
        }

        public Builder fill(ItemBuilder icon) {
            this.fill = icon;
            return this;
        }

        public PaginatedMenu build() {
            return new PaginatedMenu(this);
        }
    }
}

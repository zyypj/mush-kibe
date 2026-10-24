package br.com.muxe.core.bukkit.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.text.Colors;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class Menu {

    public static final int MAX_TITLE = 32;
    public static final int MAX_ROWS = 6;

    private final String title;
    private final int rows;
    private final Map<Integer, MenuButton> buttons;
    private final Consumer<Player> onClose;

    private Menu(String title, int rows, Map<Integer, MenuButton> buttons, Consumer<Player> onClose) {
        this.title = title;
        this.rows = rows;
        this.buttons = Collections.unmodifiableMap(new LinkedHashMap<>(buttons));
        this.onClose = onClose;
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public String title() {
        return title;
    }

    public int rows() {
        return rows;
    }

    public int size() {
        return rows * MenuLayout.ROW_WIDTH;
    }

    public Map<Integer, MenuButton> buttons() {
        return buttons;
    }

    public Optional<MenuButton> button(int slot) {
        return Optional.ofNullable(buttons.get(slot));
    }

    public Consumer<Player> onClose() {
        return onClose;
    }

    public MenuHolder open(Player player) {
        MenuHolder holder = new MenuHolder(this);
        player.openInventory(holder.getInventory());
        return holder;
    }

    public static final class Builder {

        private final String title;
        private int rows = 3;
        private final Map<Integer, MenuButton> buttons = new LinkedHashMap<>();
        private ItemBuilder border;
        private ItemBuilder fill;
        private Consumer<Player> onClose = player -> {
        };

        private Builder(String title) {
            String translated = Colors.translate(title);
            this.title = translated.length() > MAX_TITLE ? translated.substring(0, MAX_TITLE) : translated;
        }

        public Builder rows(int value) {
            if (value < 1 || value > MAX_ROWS) {
                throw new IllegalArgumentException("Menus have 1 to 6 rows, got " + value);
            }
            this.rows = value;
            return this;
        }

        public Builder button(int slot, ItemBuilder icon) {
            return button(slot, icon, MenuClick.NONE);
        }

        public Builder button(int slot, ItemBuilder icon, MenuClick click) {
            return button(slot, new MenuButton(icon, click));
        }

        public Builder button(int row, int column, ItemBuilder icon, MenuClick click) {
            return button(Slots.of(row, column), icon, click);
        }

        public Builder button(int slot, MenuButton button) {
            if (slot < 0 || slot >= rows * MenuLayout.ROW_WIDTH) {
                throw new IllegalArgumentException("Slot " + slot + " is outside a menu of " + rows + " rows");
            }
            buttons.put(slot, button);
            return this;
        }

        public Builder border(ItemBuilder icon) {
            this.border = icon;
            return this;
        }

        public Builder fill(ItemBuilder icon) {
            this.fill = icon;
            return this;
        }

        public Builder onClose(Consumer<Player> handler) {
            this.onClose = handler;
            return this;
        }

        public Menu build() {
            Map<Integer, MenuButton> placed = new LinkedHashMap<>();
            if (border != null) {
                for (int slot : Slots.border(rows)) {
                    placed.put(slot, MenuButton.of(border));
                }
            }
            buttons.forEach((slot, button) -> {
                if (slot < rows * MenuLayout.ROW_WIDTH) {
                    placed.put(slot, button);
                }
            });
            if (fill != null) {
                for (int slot = 0; slot < rows * MenuLayout.ROW_WIDTH; slot++) {
                    placed.putIfAbsent(slot, MenuButton.of(fill));
                }
            }
            return new Menu(title, rows, placed, onClose);
        }
    }
}

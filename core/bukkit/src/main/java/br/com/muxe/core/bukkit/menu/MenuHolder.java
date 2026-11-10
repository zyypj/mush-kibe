package br.com.muxe.core.bukkit.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

public final class MenuHolder implements InventoryHolder {

    private Menu menu;
    private final Map<Integer, MenuButton> buttons;
    private final Inventory inventory;
    private int age;

    MenuHolder(Menu menu) {
        this.menu = menu;
        this.buttons = new HashMap<>(menu.buttons());
        this.inventory = Bukkit.createInventory(this, menu.size(), menu.title());
        buttons.forEach((slot, button) -> inventory.setItem(slot, button.icon().build()));
    }

    public static boolean closeOpen(Player player) {
        InventoryView view = player.getOpenInventory();
        if (view != null && view.getTopInventory() != null && view.getTopInventory().getHolder() instanceof MenuHolder) {
            player.closeInventory();
            return true;
        }
        return false;
    }

    public Menu menu() {
        return menu;
    }

    public Optional<MenuButton> button(int slot) {
        return Optional.ofNullable(buttons.get(slot));
    }

    public void set(int slot, MenuButton button) {
        if (slot < 0 || slot >= menu.size()) {
            throw new IllegalArgumentException("Slot " + slot + " is outside the menu");
        }
        buttons.put(slot, button);
        inventory.setItem(slot, button.icon().build());
    }

    void replace(Menu next) {
        menu = next;
        for (int slot = 0; slot < next.size(); slot++) {
            MenuButton button = next.buttons().get(slot);
            if (button == null) {
                if (buttons.remove(slot) != null) {
                    inventory.setItem(slot, null);
                }
            } else {
                buttons.put(slot, button);
                inventory.setItem(slot, button.icon().build());
            }
        }
    }

    public void clear(int slot) {
        buttons.remove(slot);
        inventory.setItem(slot, null);
    }

    void click(Player player, int slot, ClickType type) {
        MenuButton button = buttons.get(slot);
        if (button != null) {
            button.click().click(new ClickContext(player, type, slot, this));
        }
    }

    void tick() {
        age++;
        buttons.forEach((slot, button) -> {
            if (button.changesAt(age)) {
                inventory.setItem(slot, button.iconAt(age).build());
            }
        });
    }

    void closed(Player player) {
        menu.onClose().accept(player);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

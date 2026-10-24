package br.com.muxe.core.bukkit.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {

    private final Menu menu;
    private final Map<Integer, MenuButton> buttons;
    private final Inventory inventory;

    MenuHolder(Menu menu) {
        this.menu = menu;
        this.buttons = new HashMap<>(menu.buttons());
        this.inventory = Bukkit.createInventory(this, menu.size(), menu.title());
        buttons.forEach((slot, button) -> inventory.setItem(slot, button.icon().build()));
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

    void closed(Player player) {
        menu.onClose().accept(player);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

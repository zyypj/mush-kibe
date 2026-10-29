package br.com.muxe.core.bukkit.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public record ClickContext(Player player, ClickType type, int slot, MenuHolder holder) {

    public Menu menu() {
        return holder.menu();
    }

    public boolean isLeftClick() {
        return type.isLeftClick();
    }

    public boolean isRightClick() {
        return type.isRightClick();
    }

    public boolean isShiftClick() {
        return type.isShiftClick();
    }

    public void close() {
        player.closeInventory();
    }

    public void open(Menu other) {
        other.open(player);
    }

    public void update(int targetSlot, MenuButton button) {
        holder.set(targetSlot, button);
    }

    public void updateIcon(ItemBuilder icon) {
        holder.set(slot, new MenuButton(icon, holder.button(slot).map(MenuButton::click).orElse(MenuClick.NONE)));
    }
}

package br.com.muxe.core.bukkit.parkour;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ParkourGear {

    private final Map<Integer, Optional<ItemStack>> replaced;
    private final boolean allowFlight;
    private final boolean flying;

    private ParkourGear(Map<Integer, Optional<ItemStack>> replaced, boolean allowFlight, boolean flying) {
        this.replaced = Map.copyOf(replaced);
        this.allowFlight = allowFlight;
        this.flying = flying;
    }

    public static ParkourGear equip(Player player, Map<Integer, ItemStack> items) {
        PlayerInventory inventory = player.getInventory();
        Map<Integer, Optional<ItemStack>> replaced = new HashMap<>();
        items.forEach((slot, item) -> {
            replaced.put(slot, Optional.ofNullable(inventory.getItem(slot)).map(ItemStack::clone));
            inventory.setItem(slot, item);
        });
        ParkourGear gear = new ParkourGear(replaced, player.getAllowFlight(), player.isFlying());
        player.setFlying(false);
        player.setAllowFlight(false);
        return gear;
    }

    public void restore(Player player) {
        PlayerInventory inventory = player.getInventory();
        replaced.forEach((slot, item) -> inventory.setItem(slot, item.orElse(null)));
        player.setAllowFlight(allowFlight);
        if (allowFlight) {
            player.setFlying(flying);
        }
    }
}

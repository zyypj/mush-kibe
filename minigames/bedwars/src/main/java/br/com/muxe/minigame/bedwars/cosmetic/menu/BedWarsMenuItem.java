package br.com.muxe.minigame.bedwars.cosmetic.menu;

import br.com.muxe.core.bukkit.buildmode.BuildModeHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class BedWarsMenuItem implements Listener, BuildModeHook {

    private final Plugin plugin;
    private final MenuSlot item;
    private final CosmeticMenus menus;

    public BedWarsMenuItem(Plugin plugin, MenuSlot item, CosmeticMenus menus) {
        this.plugin = plugin;
        this.item = item;
        this.menus = menus;
    }

    public int index() {
        return item.slot() - 1;
    }

    public void give(Player player) {
        player.getInventory().setItem(index(), item.item().build());
    }

    @Override
    public void left(Player player) {
        give(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                give(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack held = event.getItem();
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (!rightClick || held == null || player.getInventory().getHeldItemSlot() != index() || held.getType() != item.item().material()) {
            return;
        }
        event.setCancelled(true);
        menus.openBedWars(player);
    }
}

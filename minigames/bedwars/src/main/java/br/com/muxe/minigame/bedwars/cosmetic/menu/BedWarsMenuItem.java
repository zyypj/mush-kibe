package br.com.muxe.minigame.bedwars.cosmetic.menu;

import br.com.muxe.core.bukkit.buildmode.BuildModeHook;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.cosmetic.menu.MenuSlot;
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
    private final Localized<MenuSlot> items;
    private final LanguageLookup languages;
    private final BedWarsMenu menu;

    public BedWarsMenuItem(Plugin plugin, Localized<MenuSlot> items, LanguageLookup languages, BedWarsMenu menu) {
        this.plugin = plugin;
        this.items = items;
        this.languages = languages;
        this.menu = menu;
    }

    public int index() {
        return items.standard().slot() - 1;
    }

    public void give(Player player) {
        player.getInventory().setItem(index(), items.in(languages.of(player.getUniqueId())).item().build());
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
        if (!rightClick || held == null || player.getInventory().getHeldItemSlot() != index() || held.getType() != items.standard().item().material()) {
            return;
        }
        event.setCancelled(true);
        menu.open(player);
    }
}

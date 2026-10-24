package br.com.muxe.auth.listener;

import br.com.muxe.auth.command.AuthSetupCommand;
import br.com.muxe.auth.protection.AuthProtection;
import br.com.muxe.auth.session.AuthSessionRegistry;
import br.com.muxe.auth.setup.AuthLocations;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public final class AuthRestrictionListener implements Listener {

    private final AuthSessionRegistry sessions;
    private final AuthLocations locations;
    private final int voidLevel;
    private final MessageCatalog messages;

    public AuthRestrictionListener(AuthSessionRegistry sessions, AuthLocations locations, int voidLevel, MessageCatalog messages) {
        this.sessions = sessions;
        this.locations = locations;
        this.voidLevel = voidLevel;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!AuthProtection.mayRunCommand(isAuthenticated(player), event.getMessage())) {
            event.setCancelled(true);
            player.sendMessage(messages.get("blocked-command"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!AuthProtection.allowsGameMode(event.getNewGameMode())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to != null && AuthProtection.fellOut(to.getY(), voidLevel)) {
            Player player = event.getPlayer();
            player.setFallDistance(0);
            player.teleport(locations.spawnLocation().orElseGet(() -> player.getWorld().getSpawnLocation()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        guard(event.getDamager(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        guard(event.getRemover(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(PlayerPickupItemEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        guard(event.getWhoClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        guard(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        guard(event.getPlayer(), event);
    }

    private void guard(Entity actor, Cancellable event) {
        boolean allowed = actor instanceof Player player
            && AuthProtection.mayInteract(isAuthenticated(player), player.hasPermission(AuthSetupCommand.PERMISSION));
        if (!allowed) {
            event.setCancelled(true);
        }
    }

    private boolean isAuthenticated(Player player) {
        return sessions.isAuthenticated(player.getUniqueId());
    }
}

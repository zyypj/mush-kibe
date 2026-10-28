package br.com.muxe.lobby.listener;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.lobby.spawn.ArrivalService;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public final class LobbyProtectionListener implements Listener {

    private final ArrivalService arrival;
    private final BuildMode build;
    private final int voidLevel;

    public LobbyProtectionListener(ArrivalService arrival, BuildMode build, int voidLevel) {
        this.arrival = arrival;
        this.build = build;
        this.voidLevel = voidLevel;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent event) {
        onlyWhileBuilding(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        onlyWhileBuilding(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        onlyWhileBuilding(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPickup(PlayerPickupItemEvent event) {
        onlyWhileBuilding(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        onlyWhileBuilding(event.getWhoClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSpread(BlockSpreadEvent event) {
        if (!FireRules.spreads(event.getSource().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (!FireRules.ignites(event.getCause(), player != null && build.isBuilding(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWeather(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallIntoVoid(PlayerMoveEvent event) {
        if (event.getTo() != null && event.getTo().getY() < voidLevel) {
            Player player = event.getPlayer();
            arrival.send(player);
            if (build.isBuilding(player)) {
                player.setAllowFlight(true);
            }
        }
    }

    private void onlyWhileBuilding(HumanEntity actor, Cancellable event) {
        if (!(actor instanceof Player player) || !build.isBuilding(player)) {
            event.setCancelled(true);
        }
    }
}

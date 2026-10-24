package br.com.muxe.lobby.listener;

import br.com.muxe.lobby.spawn.ArrivalService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public final class LobbyProtectionListener implements Listener {

    public static final String BUILD_PERMISSION = "muxe.lobby.build";

    private final ArrivalService arrival;
    private final int voidLevel;

    public LobbyProtectionListener(ArrivalService arrival, int voidLevel) {
        this.arrival = arrival;
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
        if (!event.getPlayer().hasPermission(BUILD_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().hasPermission(BUILD_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        if (!event.getPlayer().hasPermission(BUILD_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPickup(PlayerPickupItemEvent event) {
        if (!event.getPlayer().hasPermission(BUILD_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getWhoClicked().hasPermission(BUILD_PERMISSION)) {
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
            arrival.send(event.getPlayer());
        }
    }
}

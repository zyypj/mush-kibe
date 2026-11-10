package br.com.muxe.afk.listener;

import br.com.muxe.afk.route.AfkReturner;
import br.com.muxe.core.bukkit.location.LocationSlot;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class AfkArrivalListener implements Listener {

    private static final int FULL_FOOD = 20;
    private static final double BLOCK_CENTER = 0.5;

    private final Server server;
    private final LocationSlot spawn;
    private final AfkReturner returner;
    private final MessageCatalog messages;

    public AfkArrivalListener(Server server, LocationSlot spawn, AfkReturner returner, MessageCatalog messages) {
        this.server = server;
        this.spawn = spawn;
        this.returner = returner;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        reset(player);
        player.teleport(spawn.location().orElseGet(this::worldSpawn));
        hideFromEveryone(player);
        returner.arrived(player.getUniqueId());
        Title.of(messages.get(player, "title"), messages.get(player, "subtitle")).send(player);
        messages.lines(player, "arrived").forEach(player::sendMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        returner.left(event.getPlayer().getUniqueId());
    }

    private void reset(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(FULL_FOOD);
        player.setFallDistance(0);
        player.setLevel(0);
        player.setExp(0);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
    }

    private void hideFromEveryone(Player player) {
        for (Player other : server.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.hidePlayer(player);
                player.hidePlayer(other);
            }
        }
    }

    private Location worldSpawn() {
        World world = server.getWorlds().getFirst();
        Location spawnPoint = world.getSpawnLocation();
        return new Location(world, spawnPoint.getBlockX() + BLOCK_CENTER, spawnPoint.getBlockY(), spawnPoint.getBlockZ() + BLOCK_CENTER);
    }
}

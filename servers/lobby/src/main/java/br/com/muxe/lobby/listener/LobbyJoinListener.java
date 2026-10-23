package br.com.muxe.lobby.listener;

import br.com.muxe.lobby.spawn.SpawnService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class LobbyJoinListener implements Listener {

    private static final double FULL_HEALTH = 20.0;
    private static final int FULL_FOOD = 20;

    private final SpawnService spawn;

    public LobbyJoinListener(SpawnService spawn) {
        this.spawn = spawn;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(FULL_HEALTH);
        player.setFoodLevel(FULL_FOOD);
        player.setExp(0);
        player.setLevel(0);
        player.teleport(spawn.location());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }
}

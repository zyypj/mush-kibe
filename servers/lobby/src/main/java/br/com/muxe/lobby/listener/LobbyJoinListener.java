package br.com.muxe.lobby.listener;

import br.com.muxe.lobby.bossbar.LobbyBossBar;
import br.com.muxe.lobby.hotbar.LobbyHotbar;
import br.com.muxe.lobby.scoreboard.LobbyScoreboard;
import br.com.muxe.lobby.spawn.ArrivalService;
import br.com.muxe.lobby.visibility.VisibilityService;
import br.com.muxe.lobby.visibility.domain.Visibility;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class LobbyJoinListener implements Listener {

    private static final double FULL_HEALTH = 20.0;
    private static final int FULL_FOOD = 20;

    private final ArrivalService arrival;
    private final LobbyHotbar hotbar;
    private final LobbyScoreboard scoreboard;
    private final LobbyBossBar bossBar;
    private final VisibilityService visibility;

    public LobbyJoinListener(ArrivalService arrival, LobbyHotbar hotbar, LobbyScoreboard scoreboard, LobbyBossBar bossBar, VisibilityService visibility) {
        this.arrival = arrival;
        this.hotbar = hotbar;
        this.scoreboard = scoreboard;
        this.bossBar = bossBar;
        this.visibility = visibility;
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
        arrival.send(player);
        hotbar.give(player, Visibility.ALL);
        player.getInventory().setHeldItemSlot(0);
        scoreboard.show(player);
        bossBar.show(player);
        visibility.joined(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        scoreboard.forget(event.getPlayer());
        visibility.left(event.getPlayer());
    }
}

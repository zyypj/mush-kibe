package br.com.muxe.lobby.listener;

import br.com.muxe.lobby.bossbar.LobbyBossBar;
import br.com.muxe.lobby.scoreboard.LobbyScoreboard;
import br.com.muxe.lobby.spawn.ArrivalService;
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
    private final LobbyScoreboard scoreboard;
    private final LobbyBossBar bossBar;

    public LobbyJoinListener(ArrivalService arrival, LobbyScoreboard scoreboard, LobbyBossBar bossBar) {
        this.arrival = arrival;
        this.scoreboard = scoreboard;
        this.bossBar = bossBar;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(FULL_HEALTH);
        player.setFoodLevel(FULL_FOOD);
        player.setExp(0);
        player.setLevel(0);
        arrival.send(player);
        scoreboard.show(player);
        bossBar.show(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        scoreboard.forget(event.getPlayer());
    }
}

package br.com.muxe.auth.listener;

import br.com.muxe.auth.flow.AuthFlow;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class AuthPlayerListener implements Listener {

    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final AuthFlow flow;
    private final MessageCatalog messages;
    private final Duration loginTimeout;

    public AuthPlayerListener(Plugin plugin, AuthFlow flow, MessageCatalog messages, Duration loginTimeout) {
        this.plugin = plugin;
        this.flow = flow;
        this.messages = messages;
        this.loginTimeout = loginTimeout;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);
        hideFromEveryone(player);
        player.teleport(player.getWorld().getSpawnLocation());
        flow.begin(player);
        scheduleTimeout(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        flow.end(event.getPlayer().getUniqueId());
    }

    private void hideFromEveryone(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.hidePlayer(player);
                player.hidePlayer(other);
            }
        }
    }

    private void scheduleTimeout(UUID playerId) {
        long ticks = Math.max(1, loginTimeout.getSeconds()) * TICKS_PER_SECOND;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && !flow.isAuthenticated(playerId)) {
                player.kickPlayer(messages.get("timeout"));
            }
        }, ticks);
    }
}

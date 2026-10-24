package br.com.muxe.core.bukkit.greeting;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class JoinGreeting implements Listener {

    private final Plugin plugin;
    private final GreetingSettings settings;
    private final MessageCatalog messages;

    public JoinGreeting(Plugin plugin, GreetingSettings settings, MessageCatalog messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> greet(player), settings.delayTicks());
    }

    private void greet(Player player) {
        if (!player.isOnline()) {
            return;
        }
        Title.builder()
            .title(messages.get("join-title"))
            .subtitle(messages.get("join-subtitle"))
            .timings(settings.fadeInTicks(), settings.stayTicks(), settings.fadeOutTicks())
            .build()
            .send(player);
        if (settings.sound() != null) {
            player.playSound(player.getLocation(), settings.sound(), settings.volume(), settings.pitch());
        }
    }
}

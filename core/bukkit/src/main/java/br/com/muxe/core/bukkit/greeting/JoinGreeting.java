package br.com.muxe.core.bukkit.greeting;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.server.ServerKind;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class JoinGreeting implements Listener {

    private final Plugin plugin;
    private final Supplier<GreetingSettings> settings;
    private final ServerKind kind;
    private final MessageCatalog messages;

    public JoinGreeting(Plugin plugin, Supplier<GreetingSettings> settings, ServerKind kind, MessageCatalog messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.kind = kind;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GreetingSettings current = settings.get();
        if (!current.appliesTo(kind)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> greet(player, current), current.delayTicks());
    }

    private void greet(Player player, GreetingSettings current) {
        if (!player.isOnline()) {
            return;
        }
        Title.builder()
            .title(messages.get(player, "join-title"))
            .subtitle(messages.get(player, "join-subtitle"))
            .timings(current.fadeInTicks(), current.stayTicks(), current.fadeOutTicks())
            .build()
            .send(player);
        if (current.sound() != null) {
            player.playSound(player.getLocation(), current.sound(), current.volume(), current.pitch());
        }
    }
}

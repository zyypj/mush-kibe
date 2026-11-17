package br.com.muxe.core.bukkit.chat;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.ChatInterval;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import java.time.Duration;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatIntervalListener implements Listener {

    public static final String FAST = "muxe.chat.fast";
    public static final String NO_COOLDOWN = "muxe.chat.no-cooldown";
    public static final String MESSAGE = "chat-cooldown";

    private final ChatInterval interval;
    private final Duration normal;
    private final Duration fast;
    private final MessageCatalog messages;

    public ChatIntervalListener(ChatInterval interval, Duration normal, Duration fast, MessageCatalog messages) {
        this.interval = interval;
        this.normal = normal;
        this.fast = fast;
        this.messages = messages;
    }

    public static ChatIntervalListener from(ConfigurationSection section, ChatInterval interval, MessageCatalog messages) {
        long normalMillis = section == null ? 3_000 : Math.round(section.getDouble("seconds", 3) * 1_000);
        long fastMillis = section == null ? 1_000 : Math.round(section.getDouble("fast-seconds", 1) * 1_000);
        return new ChatIntervalListener(interval, Duration.ofMillis(normalMillis), Duration.ofMillis(fastMillis), messages);
    }

    public Duration intervalFor(boolean noCooldown, boolean fastChat) {
        if (noCooldown) {
            return Duration.ZERO;
        }
        return fastChat ? fast : normal;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Duration wanted = intervalFor(player.hasPermission(NO_COOLDOWN), player.hasPermission(FAST));
        Optional<Duration> wait = interval.speak(player.getUniqueId(), wanted);
        if (wait.isPresent()) {
            event.setCancelled(true);
            player.sendMessage(messages.get(player, MESSAGE, "seconds", CooldownFormat.seconds(wait.get())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        interval.forget(event.getPlayer().getUniqueId());
    }
}

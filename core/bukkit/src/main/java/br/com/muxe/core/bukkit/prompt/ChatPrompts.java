package br.com.muxe.core.bukkit.prompt;

import br.com.muxe.core.common.prompt.PendingPrompts;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class ChatPrompts implements Listener {

    public static final String CANCEL_WORD = "cancelar";
    private static final long EXPIRY_CHECK_TICKS = 20L;

    private final Plugin plugin;
    private final PendingPrompts<Prompt> prompts;

    private ChatPrompts(Plugin plugin, Clock clock) {
        this.plugin = plugin;
        this.prompts = new PendingPrompts<>(clock);
    }

    public static ChatPrompts start(Plugin plugin, Clock clock) {
        ChatPrompts started = new ChatPrompts(plugin, clock);
        plugin.getServer().getPluginManager().registerEvents(started, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, started::expire, EXPIRY_CHECK_TICKS, EXPIRY_CHECK_TICKS);
        return started;
    }

    public void ask(Player player, Duration timeout, Consumer<String> onAnswer, Runnable onCancel, Runnable onTimeout) {
        prompts.open(player.getUniqueId(), timeout, new Prompt(player.getUniqueId(), onAnswer, onCancel, onTimeout));
    }

    public boolean isWaiting(Player player) {
        return prompts.isWaiting(player.getUniqueId());
    }

    public void cancel(Player player) {
        prompts.cancel(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Optional<Prompt> prompt = prompts.answer(event.getPlayer().getUniqueId());
        if (prompt.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) {
                return;
            }
            if (message.toLowerCase(Locale.ROOT).equals(CANCEL_WORD)) {
                prompt.get().onCancel().run();
            } else {
                prompt.get().onAnswer().accept(message);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        prompts.cancel(event.getPlayer().getUniqueId());
    }

    private void expire() {
        for (Prompt prompt : prompts.expire()) {
            Player player = plugin.getServer().getPlayer(prompt.player());
            if (player != null && player.isOnline()) {
                prompt.onTimeout().run();
            }
        }
    }

    private record Prompt(UUID player, Consumer<String> onAnswer, Runnable onCancel, Runnable onTimeout) {
    }
}

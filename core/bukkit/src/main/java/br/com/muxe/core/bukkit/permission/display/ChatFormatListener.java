package br.com.muxe.core.bukkit.permission.display;

import java.util.UUID;
import java.util.function.Function;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatFormatListener implements Listener {

    private final Function<UUID, RankDisplay> displays;
    private final String template;

    public ChatFormatListener(Function<UUID, RankDisplay> displays, String template) {
        this.displays = displays;
        this.template = template;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setFormat(displays.apply(event.getPlayer().getUniqueId()).chatFormat(template));
    }
}

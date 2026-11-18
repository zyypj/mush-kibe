package br.com.muxe.core.bukkit.permission.display;

import br.com.muxe.core.common.preference.domain.TagFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatFormatListener implements Listener {

    private final Function<UUID, RankDisplay> displays;
    private final Function<UUID, TagFormat> formats;
    private final String template;

    public ChatFormatListener(Function<UUID, RankDisplay> displays, Function<UUID, TagFormat> formats, String template) {
        this.displays = displays;
        this.formats = formats;
        this.template = template;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setFormat(displays.apply(event.getPlayer().getUniqueId()).chatFormat(template));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShown(AsyncPlayerChatEvent event) {
        RankDisplay sender = displays.apply(event.getPlayer().getUniqueId());
        List<Player> reformatted = new ArrayList<>();
        for (Player viewer : event.getRecipients()) {
            formatFor(event.getFormat(), sender, formats.apply(viewer.getUniqueId())).ifPresent(own -> {
                reformatted.add(viewer);
                viewer.sendMessage(String.format(own, event.getPlayer().getDisplayName(), event.getMessage()));
            });
        }
        event.getRecipients().removeAll(reformatted);
    }

    public static Optional<String> formatFor(String format, RankDisplay sender, TagFormat viewer) {
        String shown = TagFormatLook.name(sender);
        String wanted = TagFormatLook.name(TagFormatLook.apply(viewer, sender));
        if (shown.equals(wanted) || !format.contains(shown)) {
            return Optional.empty();
        }
        return Optional.of(format.replace(shown, wanted));
    }
}

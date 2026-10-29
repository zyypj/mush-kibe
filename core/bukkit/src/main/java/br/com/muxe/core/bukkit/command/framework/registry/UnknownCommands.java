package br.com.muxe.core.bukkit.command.framework.registry;

import java.util.Objects;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class UnknownCommands implements Listener {

    private final CommandMap map;
    private final String message;

    public UnknownCommands(CommandMap map, String message) {
        this.map = Objects.requireNonNull(map, "map");
        this.message = Objects.requireNonNull(message, "message");
    }

    public static String label(String typed) {
        String line = typed.startsWith("/") ? typed.substring(1) : typed;
        int space = line.indexOf(' ');
        return space < 0 ? line : line.substring(0, space);
    }

    public boolean known(String typed) {
        String label = label(typed);
        return !label.isEmpty() && map.getCommand(label) != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (known(event.getMessage())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(message);
    }
}

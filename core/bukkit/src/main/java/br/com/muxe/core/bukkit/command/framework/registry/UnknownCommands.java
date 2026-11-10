package br.com.muxe.core.bukkit.command.framework.registry;

import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Objects;
import java.util.function.Function;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class UnknownCommands implements Listener {

    public static final String UNKNOWN_COMMAND = "unknown-command";

    private final CommandMap map;
    private final Function<Player, String> unknown;
    private final Function<Player, String> denied;

    public UnknownCommands(CommandMap map, String message) {
        this(map, constant(message), null);
    }

    public UnknownCommands(CommandMap map, MessageCatalog messages) {
        this(map, player -> messages.get(player, UNKNOWN_COMMAND), player -> messages.get(player, CommandMessages.NO_PERMISSION));
    }

    public UnknownCommands(CommandMap map, Function<Player, String> unknown, Function<Player, String> denied) {
        this.map = Objects.requireNonNull(map, "map");
        this.unknown = Objects.requireNonNull(unknown, "unknown");
        this.denied = denied;
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
        String label = label(event.getMessage());
        Command command = label.isEmpty() ? null : map.getCommand(label);
        if (command == null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(unknown.apply(event.getPlayer()));
            return;
        }
        if (denied != null && !command.testPermissionSilent(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(denied.apply(event.getPlayer()));
        }
    }

    private static Function<Player, String> constant(String message) {
        Objects.requireNonNull(message, "message");
        return player -> message;
    }
}

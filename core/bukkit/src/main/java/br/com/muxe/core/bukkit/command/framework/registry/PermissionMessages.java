package br.com.muxe.core.bukkit.command.framework.registry;

import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

public final class PermissionMessages implements Listener {

    private final Plugin plugin;
    private final CommandMap map;
    private final String message;

    public PermissionMessages(Plugin plugin, CommandMap map, String message) {
        this.plugin = plugin;
        this.map = map;
        this.message = Objects.requireNonNull(message, "message");
    }

    public static int apply(CommandMap map, String message) {
        if (!(map instanceof SimpleCommandMap simple)) {
            return 0;
        }
        int changed = 0;
        for (Command command : simple.getCommands()) {
            if (!message.equals(command.getPermissionMessage())) {
                command.setPermissionMessage(message);
                changed++;
            }
        }
        return changed;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTask(plugin, () -> apply(map, message));
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> apply(map, message));
    }
}

package br.com.muxe.core.bukkit.command.framework.registry;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

public final class CommandCleanup implements Listener {

    private final Plugin plugin;
    private final CommandRegistrar registrar;

    public CommandCleanup(Plugin plugin, CommandRegistrar registrar) {
        this.plugin = plugin;
        this.registrar = registrar;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            registrar.unregisterAll();
        }
    }
}

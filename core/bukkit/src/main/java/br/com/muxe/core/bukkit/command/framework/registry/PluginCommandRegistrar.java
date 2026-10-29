package br.com.muxe.core.bukkit.command.framework.registry;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginCommandRegistrar implements CommandRegistrar {

    private final JavaPlugin plugin;
    private final List<PluginCommand> bound = new ArrayList<>();

    public PluginCommandRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(RegisteredCommand command) {
        String name = command.definition().name();
        PluginCommand declared = plugin.getCommand(name);
        if (declared == null) {
            throw new IllegalStateException("Command /" + name + " cannot be registered: the server has no reachable command map and plugin.yml does not declare it");
        }
        declared.setExecutor((sender, ignored, label, args) -> {
            command.run(sender, label, args);
            return true;
        });
        declared.setTabCompleter((sender, ignored, alias, args) -> command.complete(sender, args));
        bound.add(declared);
    }

    @Override
    public void unregisterAll() {
        bound.forEach(command -> {
            command.setExecutor(null);
            command.setTabCompleter(null);
        });
        bound.clear();
    }
}

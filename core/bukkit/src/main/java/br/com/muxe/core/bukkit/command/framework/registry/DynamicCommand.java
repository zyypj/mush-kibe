package br.com.muxe.core.bukkit.command.framework.registry;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

public final class DynamicCommand extends Command implements PluginIdentifiableCommand {

    private final Plugin plugin;
    private final RegisteredCommand command;

    public DynamicCommand(Plugin plugin, RegisteredCommand command) {
        super(command.definition().name(), command.definition().description(), "/" + command.definition().name(), new ArrayList<>(command.definition().aliases()));
        this.plugin = plugin;
        this.command = command;
        command.definition().permission().ifPresent(this::setPermission);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        command.run(sender, commandLabel, args);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return command.complete(sender, args);
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }
}

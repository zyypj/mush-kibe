package br.com.muxe.core.bukkit.command.framework.registry;

import br.com.muxe.core.bukkit.command.framework.completion.CommandCompleter;
import br.com.muxe.core.bukkit.command.framework.dispatch.CommandDispatcher;
import br.com.muxe.core.bukkit.command.framework.node.CommandDefinition;
import java.util.List;
import org.bukkit.command.CommandSender;

public record RegisteredCommand(CommandDefinition definition, CommandDispatcher dispatcher, CommandCompleter completer) {

    public void run(CommandSender sender, String label, String[] args) {
        dispatcher.dispatch(definition, sender, label, args);
    }

    public List<String> complete(CommandSender sender, String[] args) {
        return completer.complete(definition, sender, args);
    }
}

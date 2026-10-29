package br.com.muxe.core.bukkit.command.framework.argument;

import java.util.Collection;
import java.util.List;
import org.bukkit.command.CommandSender;

public interface ArgumentType<T> {

    ArgumentResult<T> parse(CommandSender sender, String input);

    default Collection<String> suggest(CommandSender sender) {
        return List.of();
    }
}

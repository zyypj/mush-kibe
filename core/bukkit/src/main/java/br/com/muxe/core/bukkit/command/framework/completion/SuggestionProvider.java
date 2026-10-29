package br.com.muxe.core.bukkit.command.framework.completion;

import java.util.Collection;
import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface SuggestionProvider {

    Collection<String> suggest(CommandSender sender);
}

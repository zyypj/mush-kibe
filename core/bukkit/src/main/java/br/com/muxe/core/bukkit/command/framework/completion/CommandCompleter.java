package br.com.muxe.core.bukkit.command.framework.completion;

import br.com.muxe.core.bukkit.command.framework.node.CommandDefinition;
import br.com.muxe.core.bukkit.command.framework.node.CommandNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;

public final class CommandCompleter {

    private final Logger logger;

    public CommandCompleter(Logger logger) {
        this.logger = logger;
    }

    public List<String> complete(CommandDefinition command, CommandSender sender, String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        int last = args.length - 1;
        CommandNode node = command.root();
        int index = 0;
        while (index < last) {
            Optional<CommandNode> child = node.child(args[index]);
            if (child.isEmpty()) {
                break;
            }
            node = child.get();
            index++;
        }
        String prefix = args[last];
        Set<String> found = new LinkedHashSet<>();
        if (index == last) {
            for (CommandNode child : node.children()) {
                if (child.methods().stream().anyMatch(method -> method.usableBy(sender))) {
                    found.add(child.name());
                    if (!prefix.isEmpty()) {
                        found.addAll(child.aliases());
                    }
                }
            }
        }
        int position = last - index;
        node.method()
            .filter(method -> method.usableBy(sender))
            .flatMap(method -> method.argumentAt(position))
            .ifPresent(argument -> {
                try {
                    found.addAll(argument.suggestions(sender));
                } catch (RuntimeException exception) {
                    logger.log(Level.WARNING, "Suggestions for /" + command.name() + " failed", exception);
                }
            });
        String lower = prefix.toLowerCase(Locale.ROOT);
        return found.stream()
            .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(lower))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }
}

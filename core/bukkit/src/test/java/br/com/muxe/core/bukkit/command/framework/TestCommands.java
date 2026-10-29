package br.com.muxe.core.bukkit.command.framework;

import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import br.com.muxe.core.bukkit.command.framework.registry.CommandRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.RegisteredCommand;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;

public final class TestCommands implements CommandRegistrar {

    public static final MessageCatalog MESSAGES = MessageCatalog.of(Map.ofEntries(
        Map.entry(CommandMessages.NO_PERMISSION, "no-permission"),
        Map.entry(CommandMessages.PLAYERS_ONLY, "players-only"),
        Map.entry(CommandMessages.CONSOLE_ONLY, "console-only"),
        Map.entry(CommandMessages.USAGE, "usage {usage}"),
        Map.entry(CommandMessages.USAGE_HEADER, "usage of /{command}"),
        Map.entry(CommandMessages.USAGE_LINE, "- {usage}"),
        Map.entry(CommandMessages.INVALID_NUMBER, "invalid number {input}"),
        Map.entry(CommandMessages.INVALID_OPTION, "invalid option {input}: {options}"),
        Map.entry(CommandMessages.PLAYER_OFFLINE, "offline {input}"),
        Map.entry(CommandMessages.FAILED, "failed"),
        Map.entry("custom-usage", List.of("custom usage", "second line"))
    ));

    private final Map<String, RegisteredCommand> registered = new LinkedHashMap<>();
    private final Commands commands;
    private int unregistered;

    public TestCommands(ArgumentTypes types, MessageCatalog messages) {
        this.commands = new Commands(types, new SuggestionRegistry(), messages, Logger.getLogger("test"), this);
    }

    public static TestCommands create() {
        return new TestCommands(ArgumentTypes.basic(), MESSAGES);
    }

    public Commands commands() {
        return commands;
    }

    public TestCommands register(Object... instances) {
        commands.register(instances);
        return this;
    }

    public void run(CommandSender sender, String line) {
        String[] words = line.split(" ", -1);
        command(words[0]).run(sender, words[0], tail(words));
    }

    public List<String> complete(CommandSender sender, String line) {
        String[] words = line.split(" ", -1);
        return command(words[0]).complete(sender, tail(words));
    }

    public RegisteredCommand command(String label) {
        RegisteredCommand found = registered.get(label);
        if (found != null) {
            return found;
        }
        return registered.values().stream()
            .filter(command -> command.definition().aliases().contains(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No command /" + label));
    }

    public int unregistered() {
        return unregistered;
    }

    @Override
    public void register(RegisteredCommand command) {
        registered.put(command.definition().name(), command);
    }

    @Override
    public void unregisterAll() {
        unregistered++;
        registered.clear();
    }

    private static String[] tail(String[] words) {
        String[] tail = new String[words.length - 1];
        System.arraycopy(words, 1, tail, 0, tail.length);
        return tail;
    }
}

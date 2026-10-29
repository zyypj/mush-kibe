package br.com.muxe.core.bukkit.command.framework;

import br.com.muxe.core.bukkit.command.framework.argument.ArgumentType;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.CommandCompleter;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionProvider;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.command.framework.dispatch.CommandDispatcher;
import br.com.muxe.core.bukkit.command.framework.node.CommandDefinition;
import br.com.muxe.core.bukkit.command.framework.registry.CommandCleanup;
import br.com.muxe.core.bukkit.command.framework.registry.CommandMapRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.CommandRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.PluginCommandRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.RegisteredCommand;
import br.com.muxe.core.bukkit.command.framework.scan.CommandScanner;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

public final class Commands {

    private final ArgumentTypes types;
    private final SuggestionRegistry suggestions;
    private final CommandDispatcher dispatcher;
    private final CommandCompleter completer;
    private final CommandRegistrar registrar;

    public Commands(ArgumentTypes types, SuggestionRegistry suggestions, MessageCatalog messages, Logger logger, CommandRegistrar registrar) {
        this.types = types;
        this.suggestions = suggestions;
        this.dispatcher = new CommandDispatcher(messages, logger);
        this.completer = new CommandCompleter(logger);
        this.registrar = registrar;
    }

    public static Commands of(JavaPlugin plugin, MessageCatalog messages) {
        Server server = plugin.getServer();
        CommandRegistrar registrar = CommandMapRegistrar.commandMap(server)
            .<CommandRegistrar>map(map -> new CommandMapRegistrar(plugin, map, plugin.getLogger()))
            .orElseGet(() -> new PluginCommandRegistrar(plugin));
        server.getPluginManager().registerEvents(new CommandCleanup(plugin, registrar), plugin);
        return new Commands(ArgumentTypes.standard(server), SuggestionRegistry.standard(server), messages, plugin.getLogger(), registrar);
    }

    public <T> Commands argument(Class<T> type, ArgumentType<? extends T> argument) {
        types.register(type, argument);
        return this;
    }

    public Commands suggestions(String name, SuggestionProvider provider) {
        suggestions.register(name, provider);
        return this;
    }

    public Commands register(Object... commands) {
        CommandScanner scanner = new CommandScanner(types, suggestions);
        for (Object command : commands) {
            for (CommandDefinition definition : scanner.scan(command)) {
                registrar.register(new RegisteredCommand(definition, dispatcher, completer));
            }
        }
        return this;
    }

    public void unregisterAll() {
        registrar.unregisterAll();
    }
}

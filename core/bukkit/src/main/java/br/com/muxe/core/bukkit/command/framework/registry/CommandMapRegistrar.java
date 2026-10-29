package br.com.muxe.core.bukkit.command.framework.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;

public final class CommandMapRegistrar implements CommandRegistrar {

    private final Plugin plugin;
    private final CommandMap map;
    private final Logger logger;
    private final List<DynamicCommand> registered = new ArrayList<>();

    public CommandMapRegistrar(Plugin plugin, CommandMap map, Logger logger) {
        this.plugin = plugin;
        this.map = map;
        this.logger = logger;
    }

    public static Optional<CommandMap> commandMap(Server server) {
        try {
            Method getter = server.getClass().getMethod("getCommandMap");
            return getter.invoke(server) instanceof CommandMap map ? Optional.of(map) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void register(RegisteredCommand command) {
        DynamicCommand dynamic = new DynamicCommand(plugin, command);
        String prefix = plugin.getName().toLowerCase(Locale.ROOT);
        if (!map.register(prefix, dynamic)) {
            logger.warning("Command /" + dynamic.getName() + " is taken by another plugin; use /" + prefix + ":" + dynamic.getName());
        }
        registered.add(dynamic);
    }

    @Override
    public void unregisterAll() {
        if (registered.isEmpty()) {
            return;
        }
        Optional<Map<String, Command>> known = knownCommands();
        if (known.isEmpty()) {
            logger.warning("Could not reach the server's known commands; " + registered.size() + " commands stay until a restart");
        }
        for (DynamicCommand command : registered) {
            known.ifPresent(commands -> commands.values().removeIf(value -> value == command));
            command.unregister(map);
        }
        registered.clear();
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Command>> knownCommands() {
        for (Class<?> type = map.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return Optional.of((Map<String, Command>) field.get(map));
            } catch (NoSuchFieldException exception) {
                continue;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}

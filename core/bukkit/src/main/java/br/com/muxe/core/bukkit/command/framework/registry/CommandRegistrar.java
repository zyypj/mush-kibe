package br.com.muxe.core.bukkit.command.framework.registry;

public interface CommandRegistrar {

    void register(RegisteredCommand command);

    void unregisterAll();
}

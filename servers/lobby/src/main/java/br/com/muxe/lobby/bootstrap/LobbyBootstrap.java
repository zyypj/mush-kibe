package br.com.muxe.lobby.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.command.PlayCommand;
import br.com.muxe.lobby.command.SetSpawnCommand;
import br.com.muxe.lobby.listener.LobbyJoinListener;
import br.com.muxe.lobby.listener.LobbyProtectionListener;
import br.com.muxe.lobby.spawn.SpawnService;
import java.util.List;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbyBootstrap {

    private final JavaPlugin plugin;

    public LobbyBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        MuxeCore core = MuxeCore.get();
        MessageCatalog messages = core.messages().merge(MessageCatalog.from(config.getConfigurationSection("messages")));
        SpawnService spawn = new SpawnService(plugin);
        List<String> games = config.getStringList("play.games");

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new LobbyJoinListener(spawn), plugin);
        plugins.registerEvents(new LobbyProtectionListener(spawn, config.getInt("protection.void-level", 0)), plugin);

        bind("jogar", new PlayCommand(core, messages, games));
        bind("setspawn", new SetSpawnCommand(spawn, messages));
    }

    private void bind(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command /" + name + " is missing from plugin.yml");
        }
        command.setExecutor(executor);
    }
}

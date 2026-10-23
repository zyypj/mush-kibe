package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.LobbyCommand;
import br.com.muxe.core.bukkit.command.MuxeAdminCommand;
import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.bukkit.monitor.BukkitServerLoad;
import br.com.muxe.core.bukkit.monitor.TickMonitor;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.thread.BukkitMainThreadExecutor;
import br.com.muxe.core.bukkit.thread.BukkitMainThreadGuard;
import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.settings.MuxeSettings;
import br.com.muxe.core.common.config.settings.SettingsLoader;
import br.com.muxe.core.common.runtime.CoreRuntime;
import br.com.muxe.core.common.runtime.PlatformHooks;
import java.util.concurrent.Executor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreBukkitBootstrap {

    private final JavaPlugin plugin;
    private CoreRuntime runtime;

    public CoreBukkitBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        MuxeSettings settings = new SettingsLoader(SecretResolver.system()).load(new BukkitConfigSource(config));
        if (settings.server().isProxy()) {
            throw new ConfigException("server.kind PROXY is only valid on Velocity");
        }

        BukkitServerLoad load = new BukkitServerLoad(plugin.getServer().getMaxPlayers());
        runtime = CoreRuntime.start(settings, new PlatformHooks(plugin.getLogger(), new BukkitMainThreadGuard(), load));

        MessageCatalog messages = MessageCatalog.from(config.getConfigurationSection("messages"));
        Executor mainThread = new BukkitMainThreadExecutor(plugin);
        NetworkGateway network = new NetworkGateway(runtime.packets());
        MuxeCore core = new BukkitMuxeCore(runtime, mainThread, network, messages);
        plugin.getServer().getServicesManager().register(MuxeCore.class, core, plugin, ServicePriority.Normal);

        plugin.getServer().getScheduler().runTaskTimer(plugin, new TickMonitor(load, runtime.metrics()), 1L, 1L);
        String lobbyGroup = config.getString("network.lobby-group", "lobby");
        bindCommand("muxe", new MuxeAdminCommand(core));
        bindCommand("lobby", new LobbyCommand(core, lobbyGroup));

        plugin.getLogger().info("Muxe-Core ready as " + settings.server().id() + " (" + settings.server().group() + ")");
    }

    public void handleStartupFailure() {
        disable();
        if (plugin.getConfig().getBoolean("startup.shutdown-on-failure", true)) {
            plugin.getLogger().severe("Shutting the server down: a Muxe server cannot run without its core.");
            plugin.getServer().shutdown();
        } else {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public void disable() {
        plugin.getServer().getServicesManager().unregisterAll(plugin);
        if (runtime != null) {
            runtime.close();
            runtime = null;
        }
    }

    private void bindCommand(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command /" + name + " is missing from plugin.yml");
        }
        command.setExecutor(executor);
    }
}

package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.buildmode.BuildSettings;
import br.com.muxe.core.bukkit.command.BuildCommand;
import br.com.muxe.core.bukkit.command.LobbyCommand;
import br.com.muxe.core.bukkit.command.MuxeAdminCommand;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.bukkit.greeting.GreetingSettings;
import br.com.muxe.core.bukkit.greeting.JoinGreeting;
import br.com.muxe.core.bukkit.menu.MenuListener;
import br.com.muxe.core.bukkit.monitor.BukkitServerLoad;
import br.com.muxe.core.bukkit.monitor.TickMonitor;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.permission.PermissionService;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.thread.BukkitMainThreadExecutor;
import br.com.muxe.core.bukkit.thread.BukkitMainThreadGuard;
import br.com.muxe.core.common.buildmode.BuildState;
import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.settings.MuxeSettings;
import br.com.muxe.core.common.config.settings.SettingsLoader;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import br.com.muxe.core.common.runtime.CoreRuntime;
import br.com.muxe.core.common.runtime.PlatformHooks;
import java.util.concurrent.Executor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreBukkitBootstrap {

    private final JavaPlugin plugin;
    private CoreRuntime runtime;
    private PlacedFeatures features;
    private PermissionService permissions;
    private RankFeatures ranks;

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
        BuildSettings buildSettings = BuildSettings.from(config.getConfigurationSection("build-mode"));
        BuildMode buildMode = new BuildMode(
            plugin.getServer(),
            new BuildState(runtime.store(), buildSettings.duration()),
            buildSettings,
            settings.server().kind(),
            runtime.executors().io(),
            mainThread,
            messages,
            plugin.getLogger()
        );
        PermissionSync permissionSync = new PermissionSync(runtime.commands(), runtime.queries(), runtime.packets(), mainThread, plugin.getLogger());
        permissions = PermissionService.start(plugin, permissionSync, settings.server().group(), runtime.clock());
        MuxeCore core = new BukkitMuxeCore(runtime, mainThread, network, messages, buildMode, permissions);
        plugin.getServer().getServicesManager().register(MuxeCore.class, core, plugin, ServicePriority.Normal);

        plugin.getServer().getScheduler().runTaskTimer(plugin, new TickMonitor(load, runtime.metrics()), 1L, 1L);
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(buildMode, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, buildMode, BuildMode.ACTION_BAR_PERIOD_TICKS, BuildMode.ACTION_BAR_PERIOD_TICKS);
        GreetingSettings greeting = GreetingSettings.from(config.getConfigurationSection("join-greeting"));
        if (greeting.appliesTo(settings.server().kind())) {
            plugin.getServer().getPluginManager().registerEvents(new JoinGreeting(plugin, greeting, messages), plugin);
        }
        String lobbyGroup = config.getString("network.lobby-group", "lobby");
        Commands commands = Commands.of(plugin, messages);
        commands.register(new MuxeAdminCommand(core), new LobbyCommand(core, lobbyGroup), new BuildCommand(buildMode, messages));
        ranks = RankFeatures.start(plugin, core, config, commands);
        features = PlacedFeatures.start(plugin, core, config, commands);
        HotbarFeatures.start(plugin, core, config, features);

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
        if (features != null) {
            features.stop();
            features = null;
        }
        if (ranks != null) {
            ranks.stop();
            ranks = null;
        }
        if (permissions != null) {
            permissions.stop();
            permissions = null;
        }
        plugin.getServer().getServicesManager().unregisterAll(plugin);
        if (runtime != null) {
            runtime.close();
            runtime = null;
        }
    }
}

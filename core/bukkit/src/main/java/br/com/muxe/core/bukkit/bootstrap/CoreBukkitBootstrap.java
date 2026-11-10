package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.buildmode.BuildSettings;
import br.com.muxe.core.bukkit.combat.knockback.KnockbackListener;
import br.com.muxe.core.bukkit.combat.knockback.KnockbackProfiles;
import br.com.muxe.core.bukkit.combat.knockback.KnockbackService;
import br.com.muxe.core.bukkit.command.BuildCommand;
import br.com.muxe.core.bukkit.command.LobbyCommand;
import br.com.muxe.core.bukkit.command.ReturnCommand;
import br.com.muxe.core.bukkit.command.config.ConfigPathSuggestions;
import br.com.muxe.core.bukkit.command.config.ConfigReplies;
import br.com.muxe.core.bukkit.command.config.CoreConfigCommand;
import br.com.muxe.core.bukkit.command.MuxeAdminCommand;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.command.framework.registry.CommandMapRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.PermissionMessages;
import br.com.muxe.core.bukkit.command.framework.registry.UnknownCommands;
import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.core.bukkit.config.edit.ConfigEditor;
import br.com.muxe.core.bukkit.config.edit.ConfigReloads;
import br.com.muxe.core.bukkit.config.edit.PluginConfigStore;
import br.com.muxe.core.bukkit.greeting.GreetingSettings;
import br.com.muxe.core.bukkit.greeting.JoinGreeting;
import br.com.muxe.core.bukkit.greeting.LeftoverTitleCleaner;
import br.com.muxe.core.bukkit.menu.MenuAnimator;
import br.com.muxe.core.bukkit.menu.MenuListener;
import br.com.muxe.core.bukkit.monitor.BukkitServerLoad;
import br.com.muxe.core.bukkit.monitor.TickMonitor;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.party.PartyClient;
import br.com.muxe.core.bukkit.nms.JoinTraffic;
import br.com.muxe.core.bukkit.permission.PermissionService;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.bukkit.thread.BukkitMainThreadExecutor;
import br.com.muxe.core.bukkit.thread.BukkitMainThreadGuard;
import br.com.muxe.core.bukkit.world.ClearMidday;
import br.com.muxe.core.common.buildmode.BuildState;
import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.live.LiveValue;
import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.settings.MuxeSettings;
import br.com.muxe.core.common.config.settings.SettingsLoader;
import br.com.muxe.core.common.match.MatchReturns;
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
        PartyClient parties = PartyFeatures.client(plugin, runtime, mainThread, messages);
        KnockbackService knockback = new KnockbackService(KnockbackProfiles.from(DefaultedSections.of(config, "knockback")));
        BukkitMuxeCore core = new BukkitMuxeCore(runtime, mainThread, network, messages, buildMode, permissions, parties, knockback);
        plugin.getServer().getServicesManager().register(MuxeCore.class, core, plugin, ServicePriority.Normal);

        plugin.getServer().getScheduler().runTaskTimer(plugin, new TickMonitor(load, runtime.metrics()), 1L, 1L);
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new KnockbackListener(knockback), plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, new MenuAnimator(), 1L, 1L);
        ClearMidday.start(plugin);
        plugin.getServer().getPluginManager().registerEvents(buildMode, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, buildMode, BuildMode.ACTION_BAR_PERIOD_TICKS, BuildMode.ACTION_BAR_PERIOD_TICKS);
        plugin.getServer().getPluginManager().registerEvents(new LeftoverTitleCleaner(Title::clear), plugin);
        LiveValue<GreetingSettings> greeting = new LiveValue<>(GreetingSettings.from(config.getConfigurationSection("join-greeting")));
        plugin.getServer().getPluginManager().registerEvents(new JoinGreeting(plugin, greeting, settings.server().kind(), messages), plugin);
        ConfigReloads reloads = new ConfigReloads()
            .register("join-greeting", "join-greeting", section -> greeting.reload(() -> GreetingSettings.from(section.getConfigurationSection("join-greeting"))));
        ConfigEditor configEditor = new ConfigEditor(new PluginConfigStore(plugin, runtime.executors().io()), reloads);
        String lobbyGroup = config.getString("network.lobby-group", "lobby");
        Commands commands = Commands.of(plugin, messages);
        ConfigPathSuggestions.register(commands, configEditor);
        commands.register(new MuxeAdminCommand(core, JoinTraffic.start(plugin, runtime.metrics())), new LobbyCommand(core, lobbyGroup), new BuildCommand(buildMode, messages),
            new ReturnCommand(core, new MatchReturns(core.store())), new CoreConfigCommand(configEditor, new ConfigReplies(messages)));
        ranks = RankFeatures.start(plugin, core, config, commands);
        PartyFeatures.start(plugin, core, parties, commands);
        features = PlacedFeatures.start(plugin, core, config, commands);
        core.customRooms(CustomRoomFeatures.kit(plugin, core, config));
        HotbarFeatures.start(plugin, core, config, features, commands);
        CommandMapRegistrar.commandMap(plugin.getServer())
            .ifPresent(map -> {
                new PermissionMessages(plugin, map, messages.get("no-permission")).start();
                plugin.getServer().getPluginManager().registerEvents(new UnknownCommands(map, messages.get("unknown-command")), plugin);
            });

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

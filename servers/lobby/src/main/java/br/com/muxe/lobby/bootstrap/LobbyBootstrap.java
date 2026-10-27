package br.com.muxe.lobby.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.bossbar.BossBars;
import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import br.com.muxe.lobby.bossbar.LobbyBossBar;
import br.com.muxe.lobby.command.PlayCommand;
import br.com.muxe.lobby.command.SetSpawnCommand;
import br.com.muxe.lobby.config.LobbySettings;
import br.com.muxe.lobby.listener.LobbyJoinListener;
import br.com.muxe.lobby.listener.LobbyProtectionListener;
import br.com.muxe.lobby.profile.PlayerInfoSource;
import br.com.muxe.lobby.scoreboard.LobbyScoreboard;
import br.com.muxe.lobby.spawn.ArrivalService;
import br.com.muxe.lobby.spawn.FlightBuildHook;
import br.com.muxe.lobby.spawn.SpawnService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class LobbyBootstrap {

    public static final String MODULE = "lobby";

    private final JavaPlugin plugin;

    public LobbyBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        MuxeCore core = MuxeCore.get();
        LobbySettings settings = LobbySettings.from(config);
        MessageCatalog messages = core.messages().merge(MessageCatalog.from(config.getConfigurationSection("messages")));

        MigrationReport migration = core.migrations().migrate(MODULE, plugin.getClass().getClassLoader());
        plugin.getLogger().info("Schema 'lobby' at version " + migration.schemaVersion());

        SpawnService spawn = new SpawnService(plugin);
        ArrivalService arrival = new ArrivalService(spawn, settings.flyHeight());
        BuildMode build = core.buildMode();
        build.addHook(new FlightBuildHook());
        LobbyScoreboard scoreboard = new LobbyScoreboard(core, settings.scoreboard(), PlayerInfoSource.fixed(settings.defaultInfo()));
        LobbyBossBar bossBar = new LobbyBossBar(BossBars.start(plugin), settings.bossBar());

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new LobbyJoinListener(arrival, scoreboard, bossBar), plugin);
        plugins.registerEvents(new LobbyProtectionListener(arrival, build, settings.voidLevel()), plugin);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        long scoreboardPeriod = LobbySettings.ticks(settings.scoreboardInterval());
        scheduler.runTaskTimer(plugin, scoreboard, scoreboardPeriod, scoreboardPeriod);
        scheduler.runTaskTimer(plugin, bossBar, bossBar.period(), bossBar.period());

        bind("jogar", new PlayCommand(core, messages, settings.games()));
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

package br.com.muxe.lobby.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.bossbar.BossBars;
import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.credit.CreditBalances;
import br.com.muxe.core.bukkit.credit.CreditBalancesListener;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import br.com.muxe.lobby.bossbar.LobbyBossBar;
import br.com.muxe.lobby.command.FlyCommand;
import br.com.muxe.lobby.command.PlayCommand;
import br.com.muxe.lobby.command.SetSpawnCommand;
import br.com.muxe.lobby.config.LobbySettings;
import br.com.muxe.lobby.listener.LobbyJoinListener;
import br.com.muxe.lobby.listener.LobbyProtectionListener;
import br.com.muxe.lobby.listener.StaticWorld;
import br.com.muxe.lobby.profile.RankInfoSource;
import br.com.muxe.lobby.scoreboard.LobbyScoreboard;
import br.com.muxe.lobby.spawn.ArrivalService;
import br.com.muxe.lobby.spawn.FlightBuildHook;
import br.com.muxe.lobby.spawn.SpawnService;
import java.time.Duration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class LobbyBootstrap {

    public static final String MODULE = "lobby";
    private static final Duration CREDIT_REFRESH = Duration.ofSeconds(30);

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

        plugin.getServer().getWorlds().forEach(StaticWorld::freeze);
        SpawnService spawn = new SpawnService(plugin);
        ArrivalService arrival = new ArrivalService(spawn, settings.flyHeight());
        BuildMode build = core.buildMode();
        build.addHook(new FlightBuildHook());
        CreditBalances credits = CreditBalancesListener.start(plugin, core, CREDIT_REFRESH);
        LobbyScoreboard scoreboard = new LobbyScoreboard(core, settings.scoreboard(),
            new RankInfoSource(player -> core.permissions().display(player).tag(), credits::of, settings.defaultInfo()));
        LobbyBossBar bossBar = new LobbyBossBar(BossBars.start(plugin), settings.bossBar());

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new LobbyJoinListener(arrival, scoreboard, bossBar), plugin);
        plugins.registerEvents(new LobbyProtectionListener(arrival, build, settings.voidLevel()), plugin);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        long scoreboardPeriod = LobbySettings.ticks(settings.scoreboardInterval());
        scheduler.runTaskTimer(plugin, scoreboard, scoreboardPeriod, scoreboardPeriod);
        scheduler.runTaskTimer(plugin, bossBar, bossBar.period(), bossBar.period());

        Commands.of(plugin, messages).register(
            new PlayCommand(core, messages, settings.games()),
            new SetSpawnCommand(spawn, messages),
            new FlyCommand(messages)
        );
    }
}

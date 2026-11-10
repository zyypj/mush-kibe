package br.com.muxe.lobby.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.bossbar.BossBars;
import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.command.config.ConfigPathSuggestions;
import br.com.muxe.core.bukkit.command.config.ConfigReplies;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.config.edit.ConfigEditor;
import br.com.muxe.core.bukkit.config.edit.PluginConfigStore;
import br.com.muxe.core.bukkit.credit.CreditBalances;
import br.com.muxe.core.bukkit.credit.CreditBalancesListener;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.language.PlayerLanguages;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.config.live.LiveValue;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import br.com.muxe.lobby.afk.AfkSettings;
import br.com.muxe.lobby.afk.AfkWatch;
import br.com.muxe.lobby.bossbar.LobbyBossBar;
import br.com.muxe.lobby.command.FlyCommand;
import br.com.muxe.lobby.command.LobbyConfigCommand;
import br.com.muxe.lobby.command.PlayCommand;
import br.com.muxe.lobby.command.SetSpawnCommand;
import br.com.muxe.lobby.config.LobbyReloads;
import br.com.muxe.lobby.config.LobbySettings;
import br.com.muxe.lobby.launchpad.LaunchPads;
import br.com.muxe.lobby.listener.LobbyJoinListener;
import br.com.muxe.lobby.listener.LobbyProtectionListener;
import br.com.muxe.lobby.listener.StaticWorld;
import br.com.muxe.lobby.listener.VoidRule;
import br.com.muxe.lobby.profile.RankInfoSource;
import br.com.muxe.lobby.scoreboard.LobbyBoard;
import br.com.muxe.lobby.scoreboard.LobbyScoreboard;
import br.com.muxe.lobby.scoreboard.ScoreboardTemplate;
import br.com.muxe.lobby.spawn.ArrivalService;
import br.com.muxe.lobby.spawn.FlightBuildHook;
import br.com.muxe.lobby.spawn.SpawnService;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.World;
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
        PlayerLanguages languages = core.languages();
        LocalizedConfig texts = LocalizedConfig.load(plugin);
        Localized<LobbySettings> localized = texts.map(LobbySettings::from);
        LobbySettings settings = localized.standard();
        MessageCatalog messages = core.messages().merge(MessageCatalog.from(texts, "messages"));

        MigrationReport migration = core.migrations().migrate(MODULE, plugin.getClass().getClassLoader());
        plugin.getLogger().info("Schema 'lobby' at version " + migration.schemaVersion());

        plugin.getServer().getWorlds().forEach(StaticWorld::freeze);
        SpawnService spawn = new SpawnService(plugin);
        ArrivalService arrival = new ArrivalService(spawn, settings.flyHeight());
        BuildMode build = core.buildMode();
        build.addHook(new FlightBuildHook());
        CreditBalances credits = CreditBalancesListener.start(plugin, core, CREDIT_REFRESH);
        LiveValue<Localized<ScoreboardTemplate>> scoreboardTemplates = new LiveValue<>(localized.map(LobbySettings::scoreboard));
        ConfigEditor configEditor = new ConfigEditor(new PluginConfigStore(plugin, core.async()),
            LobbyReloads.create(scoreboardTemplates, edited -> LocalizedConfig.load(plugin), spawn::reload));
        RankInfoSource info = new RankInfoSource(player -> core.permissions().display(player).tag(), credits::of, localized.map(LobbySettings::defaultInfo), languages);
        LobbyScoreboard scoreboard = new LobbyScoreboard(core, new LobbyBoard(scoreboardTemplates, languages, info));
        LobbyBossBar bossBar = new LobbyBossBar(BossBars.start(plugin), localized.map(LobbySettings::bossBar), languages);
        languages.onChange(player -> {
            scoreboard.refresh(player);
            bossBar.show(player);
        });

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new LobbyJoinListener(arrival, scoreboard, bossBar), plugin);
        plugins.registerEvents(new LobbyProtectionListener(arrival, build, new VoidRule(lobbyWorlds(), settings.voidLevel())), plugin);
        LaunchPads.start(plugin, core, settings.launchPad());
        AfkWatch.start(plugin, core, AfkSettings.from(config.getConfigurationSection("afk")));

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        long scoreboardPeriod = LobbySettings.ticks(settings.scoreboardInterval());
        scheduler.runTaskTimer(plugin, scoreboard, scoreboardPeriod, scoreboardPeriod);
        scheduler.runTaskTimer(plugin, bossBar, bossBar.period(), bossBar.period());

        Commands commands = Commands.of(plugin, messages);
        ConfigPathSuggestions.register(commands, configEditor);
        commands.register(
            new PlayCommand(core, messages, settings.games()),
            new SetSpawnCommand(spawn, messages),
            new FlyCommand(messages),
            new LobbyConfigCommand(configEditor, new ConfigReplies(messages))
        );
    }

    private Set<String> lobbyWorlds() {
        return plugin.getServer().getWorlds().stream().map(World::getName).collect(Collectors.toUnmodifiableSet());
    }
}

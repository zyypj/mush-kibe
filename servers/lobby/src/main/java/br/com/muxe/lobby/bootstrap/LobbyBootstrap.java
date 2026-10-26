package br.com.muxe.lobby.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.bossbar.BossBars;
import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.npc.skin.MojangSkins;
import br.com.muxe.core.bukkit.prompt.ChatPrompts;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import br.com.muxe.lobby.bossbar.LobbyBossBar;
import br.com.muxe.lobby.command.NpcCommand;
import br.com.muxe.lobby.command.PlayCommand;
import br.com.muxe.lobby.command.SetSpawnCommand;
import br.com.muxe.lobby.command.ShopCommand;
import br.com.muxe.lobby.config.LobbySettings;
import br.com.muxe.lobby.hotbar.LobbyBuildHook;
import br.com.muxe.lobby.hotbar.LobbyHotbar;
import br.com.muxe.lobby.listener.HotbarListener;
import br.com.muxe.lobby.listener.LobbyJoinListener;
import br.com.muxe.lobby.listener.LobbyProtectionListener;
import br.com.muxe.lobby.npc.LobbyNpcs;
import br.com.muxe.lobby.npc.NpcStore;
import br.com.muxe.lobby.profile.PlayerInfoSource;
import br.com.muxe.lobby.scoreboard.LobbyScoreboard;
import br.com.muxe.lobby.selector.LobbySelectorMenu;
import br.com.muxe.lobby.shop.ShopService;
import br.com.muxe.lobby.shop.register.ShopRegister;
import br.com.muxe.lobby.spawn.ArrivalService;
import br.com.muxe.lobby.spawn.SpawnService;
import br.com.muxe.lobby.visibility.VisibilityModule;
import br.com.muxe.lobby.visibility.VisibilityService;
import br.com.muxe.lobby.visibility.domain.FriendDirectory;
import br.com.muxe.lobby.visibility.infrastructure.postgres.PostgresVisibilityRepository;
import java.io.File;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class LobbyBootstrap {

    public static final String MODULE = "lobby";
    private static final String NPC_FILE = "npcs.yml";

    private final JavaPlugin plugin;
    private LobbyNpcs npcs;
    private ShopService shop;
    private ShopRegister register;

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
        VisibilityModule.register(core.commands(), core.queries(), new PostgresVisibilityRepository(), core.transactions(), core.clock());

        SpawnService spawn = new SpawnService(plugin);
        ArrivalService arrival = new ArrivalService(spawn, settings.flyHeight());
        SpamGuard spam = new SpamGuard(core.clock(), settings.spamMaxClicks(), settings.spamWindow(), settings.spamCooldown());
        LobbyHotbar hotbar = new LobbyHotbar(settings.hotbar());
        VisibilityService visibility = new VisibilityService(core, FriendDirectory.NONE, hotbar, messages, plugin.getLogger());
        LobbySelectorMenu selector = new LobbySelectorMenu(core, settings.selector(), messages);
        BuildMode build = core.buildMode();
        build.addHook(new LobbyBuildHook(hotbar, visibility));
        LobbyScoreboard scoreboard = new LobbyScoreboard(core, settings.scoreboard(), PlayerInfoSource.fixed(settings.defaultInfo()));
        LobbyBossBar bossBar = new LobbyBossBar(BossBars.start(plugin), settings.bossBar());
        npcs = new LobbyNpcs(plugin, core, new NpcStore(new File(plugin.getDataFolder(), NPC_FILE), plugin.getLogger()), MojangSkins.standard(), spam, messages, settings.npcLines());
        npcs.load();
        shop = new ShopService(plugin, settings.shop(), messages, ChatPrompts.start(plugin, core.clock()), spam);
        register = new ShopRegister(plugin, settings.shop().register(), shop::openFeatured);
        register.place();

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new LobbyJoinListener(arrival, hotbar, scoreboard, bossBar, visibility), plugin);
        plugins.registerEvents(new LobbyProtectionListener(arrival, build, settings.voidLevel()), plugin);
        plugins.registerEvents(new HotbarListener(hotbar, spam, visibility, selector, build, messages), plugin);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        long scoreboardPeriod = LobbySettings.ticks(settings.scoreboardInterval());
        scheduler.runTaskTimer(plugin, scoreboard, scoreboardPeriod, scoreboardPeriod);
        scheduler.runTaskTimer(plugin, bossBar, bossBar.period(), bossBar.period());

        bind("jogar", new PlayCommand(core, messages, settings.games()));
        bind("setspawn", new SetSpawnCommand(spawn, messages));
        bind(NpcCommand.NAME, new NpcCommand(core, npcs, messages));
        bind(ShopCommand.NAME, new ShopCommand(shop, register, messages));
    }

    public void disable() {
        if (npcs != null) {
            npcs.stop();
        }
        if (register != null) {
            register.stop();
        }
        if (shop != null) {
            shop.stop();
        }
    }

    private void bind(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command /" + name + " is missing from plugin.yml");
        }
        command.setExecutor(executor);
    }
}

package br.com.muxe.minigame.bedwars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.arena.BedWarsSchema;
import br.com.muxe.minigame.bedwars.command.BedWarsCommand;
import br.com.muxe.minigame.bedwars.listener.MatchListener;
import br.com.muxe.minigame.bedwars.room.RoomService;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog;
import br.com.muxe.minigame.bedwars.shop.ShopService;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog;
import br.com.muxe.minigame.engine.MinigameEngine;
import br.com.muxe.minigame.engine.arena.ArenaCatalog;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.GameModeLoader;
import br.com.muxe.minigame.engine.setup.ArenaFiles;
import br.com.muxe.minigame.engine.setup.session.SetupCommand;
import br.com.muxe.minigame.engine.setup.session.SetupMessages;
import br.com.muxe.minigame.engine.setup.session.SetupService;
import br.com.muxe.minigame.engine.world.server.MatchWorld;
import br.com.muxe.minigame.engine.world.server.MuxeWorlds;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedWarsBootstrap {

    public static final String GAME = "bedwars";

    private final JavaPlugin plugin;
    private SetupService setup;
    private RoomService rooms;

    public BedWarsBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public MinigameEngine enable() {
        plugin.saveDefaultConfig();
        MuxeCore core = MuxeCore.get();
        FileConfiguration config = plugin.getConfig();
        MessageCatalog messages = SetupMessages.withOverrides(core.messages().merge(MessageCatalog.from(config.getConfigurationSection("messages"))));
        ConfigurationSection modeSection = Objects.requireNonNull(config.getConfigurationSection("modes"), "Missing configuration section modes");
        List<GameMode> modes = GameModeLoader.load(modeSection);
        BedWarsSchema schema = BedWarsSchema.from(modes, modeSection);
        BedWarsSettings settings = BedWarsSettings.from(config);
        MinigameEngine engine = new MinigameEngine(GAME, modes, core.metrics());
        Path data = plugin.getDataFolder().toPath();

        MuxeWorlds worlds = new MuxeWorlds(plugin);
        worlds.deleteLeftovers();
        MatchWorld matchWorld = new MatchWorld(plugin, worlds, GAME);
        ArenaCatalog catalog = new ArenaCatalog(new ArenaFiles(data.resolve("arenas")), data.resolve("maps"), matchWorld::fits, plugin.getLogger());
        try {
            plugin.getLogger().info("Loaded " + catalog.loadAll() + " ready arenas");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not load the arenas", exception);
        }

        rooms = new RoomService(plugin, core, settings, messages, schema, catalog, matchWorld);
        rooms.start();
        ShopService shop = new ShopService(
            ShopCatalog.from(Objects.requireNonNull(config.getConfigurationSection("shop"), "Missing configuration section shop")),
            UpgradeCatalog.from(Objects.requireNonNull(config.getConfigurationSection("upgrades"), "Missing configuration section upgrades")),
            messages
        );
        rooms.onNpcClick((player, click) -> shop.open(player, click.runtime(), click.upgrades()));
        plugin.getServer().getPluginManager().registerEvents(new MatchListener(plugin, core, rooms, settings, messages), plugin);

        setup = new SetupService(plugin, schema, messages, worlds, data);
        setup.onSaved(rooms::arenaSaved);
        setup.start();
        rooms.busyWhen(player -> setup.sessionOf(player).isPresent());
        Commands.of(plugin, messages).register(new SetupCommand(setup), new BedWarsCommand(rooms, messages));

        plugin.getLogger().info("BedWars ready with modes " + engine.modes().keySet());
        return engine;
    }

    public void disable() {
        if (setup != null) {
            setup.stop();
        }
        if (rooms != null) {
            rooms.stop();
        }
    }
}

package br.com.muxe.minigame.bedwars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.arena.BedWarsSchema;
import br.com.muxe.minigame.engine.MinigameEngine;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.GameModeLoader;
import br.com.muxe.minigame.engine.setup.session.SetupCommand;
import br.com.muxe.minigame.engine.setup.session.SetupMessages;
import br.com.muxe.minigame.engine.setup.session.SetupService;
import br.com.muxe.minigame.engine.world.server.MuxeWorlds;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedWarsBootstrap {

    public static final String GAME = "bedwars";

    private final JavaPlugin plugin;
    private SetupService setup;

    public BedWarsBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public MinigameEngine enable() {
        plugin.saveDefaultConfig();
        MuxeCore core = MuxeCore.get();
        FileConfiguration config = plugin.getConfig();
        MessageCatalog messages = SetupMessages.withOverrides(core.messages().merge(MessageCatalog.from(config.getConfigurationSection("messages"))));
        ConfigurationSection modeSection = config.getConfigurationSection("modes");
        List<GameMode> modes = GameModeLoader.load(modeSection);
        BedWarsSchema schema = BedWarsSchema.from(modes, modeSection);
        MinigameEngine engine = new MinigameEngine(GAME, modes, core.metrics());

        MuxeWorlds worlds = new MuxeWorlds(plugin);
        worlds.deleteLeftovers();
        setup = new SetupService(plugin, schema, messages, worlds, plugin.getDataFolder().toPath());
        setup.start();
        Commands.of(plugin, messages).register(new SetupCommand(setup));

        plugin.getLogger().info("BedWars ready with modes " + engine.modes().keySet());
        return engine;
    }

    public void disable() {
        if (setup != null) {
            setup.stop();
        }
    }
}

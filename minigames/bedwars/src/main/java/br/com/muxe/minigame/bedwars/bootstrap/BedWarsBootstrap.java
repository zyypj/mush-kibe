package br.com.muxe.minigame.bedwars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.minigame.engine.MinigameEngine;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.GameModeLoader;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedWarsBootstrap {

    public static final String GAME = "bedwars";

    private final JavaPlugin plugin;

    public BedWarsBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public MinigameEngine enable() {
        plugin.saveDefaultConfig();
        MuxeCore core = MuxeCore.get();
        List<GameMode> modes = GameModeLoader.load(plugin.getConfig().getConfigurationSection("modes"));
        MinigameEngine engine = new MinigameEngine(GAME, modes, core.metrics());
        plugin.getLogger().info("BedWars ready with modes " + engine.modes().keySet());
        return engine;
    }
}

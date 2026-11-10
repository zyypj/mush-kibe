package br.com.muxe.minigame.skywars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.api.bootstrap.MinigameBootstrap;
import br.com.muxe.minigame.engine.MinigameEngine;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.GameModeLoader;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyWarsBootstrap {

    public static final String GAME = "skywars";

    private final JavaPlugin plugin;

    public SkyWarsBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public MinigameEngine enable() {
        plugin.saveDefaultConfig();
        MuxeCore core = MuxeCore.get();
        LocalizedConfig texts = LocalizedConfig.load(plugin);
        MessageCatalog messages = core.messages().merge(MessageCatalog.from(texts, "messages"));
        List<GameMode> modes = GameModeLoader.modes(texts);
        MinigameBootstrap minigame = new MinigameBootstrap(plugin, core, GAME, modes, messages, Commands.of(plugin, messages), texts);
        plugin.getLogger().info("SkyWars ready with modes " + minigame.engine().modes().keySet());
        return minigame.engine();
    }
}

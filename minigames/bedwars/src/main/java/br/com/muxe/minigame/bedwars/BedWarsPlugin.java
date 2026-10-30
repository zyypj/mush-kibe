package br.com.muxe.minigame.bedwars;

import br.com.muxe.minigame.bedwars.bootstrap.BedWarsBootstrap;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedWarsPlugin extends JavaPlugin {

    private BedWarsBootstrap bootstrap;

    @Override
    public void onEnable() {
        try {
            bootstrap = new BedWarsBootstrap(this);
            bootstrap.enable();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Muxe-BedWars failed to start", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
    }
}

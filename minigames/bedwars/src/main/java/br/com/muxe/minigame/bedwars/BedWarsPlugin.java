package br.com.muxe.minigame.bedwars;

import br.com.muxe.minigame.bedwars.bootstrap.BedWarsBootstrap;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedWarsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            new BedWarsBootstrap(this).enable();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Muxe-BedWars failed to start", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}

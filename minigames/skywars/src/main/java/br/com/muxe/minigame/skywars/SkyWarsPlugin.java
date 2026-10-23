package br.com.muxe.minigame.skywars;

import br.com.muxe.minigame.skywars.bootstrap.SkyWarsBootstrap;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyWarsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            new SkyWarsBootstrap(this).enable();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Muxe-SkyWars failed to start", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}

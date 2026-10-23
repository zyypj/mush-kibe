package br.com.muxe.core.bukkit;

import br.com.muxe.core.bukkit.bootstrap.CoreBukkitBootstrap;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class MuxeCorePlugin extends JavaPlugin {

    private CoreBukkitBootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new CoreBukkitBootstrap(this);
        try {
            bootstrap.enable();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Muxe-Core failed to start", exception);
            bootstrap.handleStartupFailure();
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
    }
}

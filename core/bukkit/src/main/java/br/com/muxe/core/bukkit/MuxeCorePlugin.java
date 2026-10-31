package br.com.muxe.core.bukkit;

import br.com.muxe.core.bukkit.bootstrap.ClassPreloader;
import br.com.muxe.core.bukkit.bootstrap.CoreBukkitBootstrap;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class MuxeCorePlugin extends JavaPlugin {

    private static final String CORE_PACKAGE = "br.com.muxe.core";

    private CoreBukkitBootstrap bootstrap;

    @Override
    public void onLoad() {
        try {
            ClassPreloader.preload(getClassLoader(), getFile(), CORE_PACKAGE);
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Could not preload the core classes", exception);
        }
    }

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

package br.com.muxe.auth;

import br.com.muxe.auth.bootstrap.AuthBootstrap;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class MuxeAuthPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            new AuthBootstrap(this).enable();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Muxe-Auth failed to start, shutting down", exception);
            getServer().shutdown();
        }
    }
}

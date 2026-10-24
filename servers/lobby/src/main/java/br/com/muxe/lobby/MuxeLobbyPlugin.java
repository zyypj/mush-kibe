package br.com.muxe.lobby;

import br.com.muxe.lobby.bootstrap.LobbyBootstrap;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class MuxeLobbyPlugin extends JavaPlugin {

    private LobbyBootstrap bootstrap;

    @Override
    public void onEnable() {
        try {
            bootstrap = new LobbyBootstrap(this);
            bootstrap.enable();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Muxe-Lobby failed to start", exception);
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

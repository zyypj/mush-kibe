package br.com.muxe.minigame.bedwars.cosmetic.profile;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CosmeticProfileListener implements Listener {

    private final CosmeticProfiles profiles;
    private final Logger logger;

    public CosmeticProfileListener(CosmeticProfiles profiles, Logger logger) {
        this.profiles = profiles;
        this.logger = logger;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.forget(event.getPlayer().getUniqueId());
    }

    public void load(Player player) {
        profiles.load(player.getUniqueId(), CosmeticAccess.of(player::hasPermission)).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not load the cosmetics of " + player.getName(), error);
            return null;
        });
    }
}

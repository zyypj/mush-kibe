package br.com.muxe.minigame.bedwars.listener;

import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.runtime.Matches;
import br.com.muxe.minigame.bedwars.game.SpiderWeb;
import br.com.muxe.minigame.bedwars.room.BedWarsMatch;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;

public final class SpecialProjectiles implements Listener {

    private final Matches<BedWarsMatch> rooms;

    public SpecialProjectiles(Matches<BedWarsMatch> rooms) {
        this.rooms = rooms;
    }

    @EventHandler
    public void onEggThrow(PlayerEggThrowEvent event) {
        if (rooms.of(event.getPlayer().getUniqueId()).isPresent()) {
            event.setHatching(false);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball shot) || !shot.hasMetadata(SpiderWeb.SNOWBALL_TAG) || !(shot.getShooter() instanceof Player owner)) {
            return;
        }
        rooms.of(owner.getUniqueId())
            .filter(runtime -> runtime.match().phase() == MatchPhase.PLAYING)
            .ifPresent(runtime -> runtime.state().specials().spiderLanded(shot.getLocation()));
    }
}

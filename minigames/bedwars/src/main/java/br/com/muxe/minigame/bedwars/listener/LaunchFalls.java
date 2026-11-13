package br.com.muxe.minigame.bedwars.listener;

import br.com.muxe.minigame.api.runtime.Matches;
import br.com.muxe.minigame.bedwars.game.FallCap;
import br.com.muxe.minigame.bedwars.room.BedWarsMatch;
import java.time.Clock;
import java.time.Instant;
import java.util.OptionalLong;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public final class LaunchFalls implements Listener {

    public static final String METADATA = "muxe-launched-at";
    private static final long CLEAR_DELAY_TICKS = 2L;

    private final Plugin plugin;
    private final Matches<BedWarsMatch> rooms;
    private final Clock clock;

    public LaunchFalls(Plugin plugin, Matches<BedWarsMatch> rooms, Clock clock) {
        this.plugin = plugin;
        this.rooms = rooms;
        this.clock = clock;
    }

    public static void mark(Plugin plugin, Player player, Instant now) {
        player.setMetadata(METADATA, new FixedMetadataValue(plugin, now.toEpochMilli()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || rooms.of(player.getUniqueId()).isEmpty()) {
            return;
        }
        if (event.getCause() == DamageCause.FALL) {
            OptionalLong launchedAt = launchedAt(player);
            if (launchedAt.isPresent()) {
                player.removeMetadata(METADATA, plugin);
                event.setDamage(FallCap.damage(event.getDamage(), launchedAt.getAsLong(), clock.millis()));
            }
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity && (byEntity.getDamager() instanceof Fireball || byEntity.getDamager() instanceof TNTPrimed)) {
            mark(plugin, player, clock.instant());
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        OptionalLong launchedAt = launchedAt(player);
        if (launchedAt.isEmpty() || !FallCap.landed(launchedAt.getAsLong(), clock.millis(), player.isOnGround())) {
            return;
        }
        long seen = launchedAt.getAsLong();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (launchedAt(player).equals(OptionalLong.of(seen)) && player.isOnGround()) {
                player.removeMetadata(METADATA, plugin);
            }
        }, CLEAR_DELAY_TICKS);
    }

    private OptionalLong launchedAt(Player player) {
        for (MetadataValue value : player.getMetadata(METADATA)) {
            if (plugin.equals(value.getOwningPlugin())) {
                return OptionalLong.of(value.asLong());
            }
        }
        return OptionalLong.empty();
    }
}

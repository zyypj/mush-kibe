package br.com.muxe.minigame.bedwars.listener;

import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.Matches;
import br.com.muxe.minigame.bedwars.game.SpiderWeb;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class Bedbugs implements Listener {

    public static final String ITEM_ID = "percevejo";
    public static final Duration LIFE = Duration.ofSeconds(15);
    public static final double CHASE_RANGE = 16;
    public static final long TICK_PERIOD = 10L;

    private final Plugin plugin;
    private final Matches<?> rooms;
    private final Clock clock;
    private final Map<UUID, Bug> bugs = new HashMap<>();
    private BukkitTask task;

    public Bedbugs(Plugin plugin, Matches<?> rooms, Clock clock) {
        this.plugin = plugin;
        this.rooms = rooms;
        this.clock = clock;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_PERIOD, TICK_PERIOD);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        bugs.values().forEach(bug -> bug.entity().remove());
        bugs.clear();
    }

    public Optional<Bug> bug(Entity entity) {
        return Optional.ofNullable(bugs.get(entity.getUniqueId()));
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball) || snowball.hasMetadata(SpiderWeb.SNOWBALL_TAG) || !(snowball.getShooter() instanceof Player owner)) {
            return;
        }
        Optional<? extends MatchRuntime<?>> found = rooms.of(owner.getUniqueId());
        if (found.isEmpty() || found.get().match().phase() != MatchPhase.PLAYING) {
            return;
        }
        Optional<Participant> player = found.get().match().player(owner.getUniqueId()).filter(Participant::isAlive);
        if (player.isEmpty()) {
            return;
        }
        Silverfish silverfish = (Silverfish) snowball.getWorld().spawnEntity(snowball.getLocation(), EntityType.SILVERFISH);
        bugs.put(silverfish.getUniqueId(), new Bug(silverfish, owner.getUniqueId(), player.get().team(), clock.instant().plus(LIFE)));
        retarget(bugs.get(silverfish.getUniqueId()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        Bug bug = bugs.get(event.getEntity().getUniqueId());
        if (bug != null && !(event.getTarget() instanceof Player target && enemy(bug, target))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlock(EntityChangeBlockEvent event) {
        if (bugs.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (bugs.remove(event.getEntity().getUniqueId()) != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    private void tick() {
        Instant now = clock.instant();
        Iterator<Bug> iterator = bugs.values().iterator();
        while (iterator.hasNext()) {
            Bug bug = iterator.next();
            if (!bug.entity().isValid() || now.isAfter(bug.expires())) {
                bug.entity().remove();
                iterator.remove();
                continue;
            }
            retarget(bug);
        }
    }

    private void retarget(Bug bug) {
        Silverfish entity = bug.entity();
        if (entity.getTarget() instanceof Player current && enemy(bug, current)) {
            return;
        }
        entity.getNearbyEntities(CHASE_RANGE, CHASE_RANGE, CHASE_RANGE).stream()
            .filter(nearby -> nearby instanceof Player)
            .map(Player.class::cast)
            .filter(player -> enemy(bug, player))
            .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(entity.getLocation())))
            .ifPresent(entity::setTarget);
    }

    private boolean enemy(Bug bug, Player target) {
        return rooms.of(target.getUniqueId())
            .flatMap(runtime -> runtime.match().player(target.getUniqueId()))
            .filter(player -> player.isAlive() && player.team() != bug.team())
            .isPresent();
    }

    public record Bug(Silverfish entity, UUID owner, TeamColor team, Instant expires) {
    }
}

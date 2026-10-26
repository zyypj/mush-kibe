package br.com.muxe.core.bukkit.display;

import br.com.muxe.core.bukkit.hologram.HologramTransport;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.nms.EntityUse;
import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import br.com.muxe.core.bukkit.nms.PacketInterceptor;
import br.com.muxe.core.bukkit.nms.PacketStandTransport;
import br.com.muxe.core.bukkit.npc.NpcRange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class DisplayService implements Listener {

    public static final long RANGE_CHECK_TICKS = 10L;

    private final Plugin plugin;
    private final PacketStandTransport stands;
    private final HologramTransport holograms;
    private final Map<String, Placed> displays = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, SpawnedHologram>> viewers = new ConcurrentHashMap<>();
    private final Optional<PacketInterceptor> interceptor;
    private final BukkitTask task;
    private volatile BiConsumer<Player, Display> clickHandler = (player, display) -> {
    };
    private long tick;

    private DisplayService(Plugin plugin, PacketStandTransport stands, HologramTransport holograms) {
        this.plugin = plugin;
        this.stands = stands;
        this.holograms = holograms;
        this.interceptor = PacketInterceptor.start(plugin, this::onUse);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public static DisplayService start(Plugin plugin) {
        return new DisplayService(plugin, PacketStandTransport.instance(), PacketHologramTransport.instance());
    }

    public void onClick(BiConsumer<Player, Display> handler) {
        this.clickHandler = handler;
    }

    public void put(Display display) {
        remove(display.id());
        List<Body> bodies = new ArrayList<>(display.stands().size());
        for (DisplayStand stand : display.stands()) {
            bodies.add(new Body(stand, stands.create(stand.at(display.anchor(), tick), stand)));
        }
        displays.put(display.id(), new Placed(display, List.copyOf(bodies)));
    }

    public void remove(String id) {
        Placed removed = displays.remove(id);
        if (removed == null) {
            return;
        }
        for (Map.Entry<UUID, Map<String, SpawnedHologram>> entry : viewers.entrySet()) {
            SpawnedHologram labels = entry.getValue().remove(id);
            Player viewer = plugin.getServer().getPlayer(entry.getKey());
            if (labels != null && viewer != null) {
                hide(viewer, removed, labels);
            }
        }
    }

    public Optional<Display> find(String id) {
        return Optional.ofNullable(displays.get(id)).map(Placed::display);
    }

    public void stop() {
        task.cancel();
        interceptor.ifPresent(PacketInterceptor::stop);
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            Map<String, SpawnedHologram> seen = viewers.remove(viewer.getUniqueId());
            if (seen != null) {
                seen.forEach((id, labels) -> Optional.ofNullable(displays.get(id)).ifPresent(placed -> hide(viewer, placed, labels)));
            }
        }
    }

    private void tick() {
        tick++;
        if (tick % RANGE_CHECK_TICKS == 0) {
            updateRanges();
        }
        animate();
    }

    private void updateRanges() {
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            Map<String, SpawnedHologram> seen = viewers.computeIfAbsent(viewer.getUniqueId(), ignored -> new ConcurrentHashMap<>());
            for (Placed placed : displays.values()) {
                String id = placed.display().id();
                switch (NpcRange.decide(seen.containsKey(id), distanceSquared(viewer, placed.display().anchor()))) {
                    case SHOW -> seen.put(id, show(viewer, placed));
                    case HIDE -> hide(viewer, placed, seen.remove(id));
                    case KEEP -> {
                    }
                }
            }
        }
    }

    private void animate() {
        for (Placed placed : displays.values()) {
            Collection<Player> watching = watching(placed.display().id());
            for (Body body : placed.bodies()) {
                if (body.stand().motion().moves()) {
                    body.body().move(body.stand().at(placed.display().anchor(), tick), watching);
                }
            }
        }
    }

    private Collection<Player> watching(String id) {
        List<Player> watching = new ArrayList<>();
        viewers.forEach((uuid, seen) -> {
            if (seen.containsKey(id)) {
                Player viewer = plugin.getServer().getPlayer(uuid);
                if (viewer != null) {
                    watching.add(viewer);
                }
            }
        });
        return watching;
    }

    private SpawnedHologram show(Player viewer, Placed placed) {
        placed.bodies().forEach(body -> body.body().show(viewer));
        return holograms.spawn(viewer, placed.display().labelPixels());
    }

    private void hide(Player viewer, Placed placed, SpawnedHologram labels) {
        placed.bodies().forEach(body -> body.body().hide(viewer));
        if (labels != null) {
            labels.remove();
        }
    }

    private void onUse(Player player, int entityId, EntityUse use) {
        if (use == EntityUse.INTERACT_AT) {
            return;
        }
        for (Placed placed : displays.values()) {
            if (placed.owns(entityId)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        clickHandler.accept(player, placed.display());
                    }
                });
                return;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        viewers.remove(event.getPlayer().getUniqueId());
    }

    private static double distanceSquared(Player viewer, Location anchor) {
        Location at = viewer.getLocation();
        if (anchor.getWorld() == null || !anchor.getWorld().equals(at.getWorld())) {
            return Double.POSITIVE_INFINITY;
        }
        return at.distanceSquared(anchor);
    }

    private record Body(DisplayStand stand, StandBody body) {
    }

    private record Placed(Display display, List<Body> bodies) {

        private boolean owns(int entityId) {
            return bodies.stream().anyMatch(body -> body.body().entityId() == entityId);
        }
    }
}

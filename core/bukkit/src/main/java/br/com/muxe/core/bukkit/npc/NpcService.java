package br.com.muxe.core.bukkit.npc;

import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.core.bukkit.nms.EntityUse;
import br.com.muxe.core.bukkit.nms.PacketInterceptor;
import br.com.muxe.core.bukkit.nms.PacketNpcTransport;
import br.com.muxe.core.bukkit.text.Colors;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NpcService implements Listener {

    public static final long TICK_PERIOD = 10L;
    public static final long TAB_LIST_DELAY_TICKS = 40L;
    public static final double HOLOGRAM_ABOVE_HEAD = 2.05;
    public static final String HIDDEN_NAMES_TEAM = "muxe-npcs";

    private final Plugin plugin;
    private final PacketNpcTransport transport;
    private final TextHolograms holograms;
    private final Function<Npc, List<String>> lines;
    private final Map<String, Placed> npcs = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Seen>> viewers = new ConcurrentHashMap<>();
    private final Optional<PacketInterceptor> interceptor;
    private volatile BiConsumer<Player, Npc> clickHandler = (player, npc) -> {
    };

    private NpcService(Plugin plugin, PacketNpcTransport transport, TextHolograms holograms, Function<Npc, List<String>> lines) {
        this.plugin = plugin;
        this.transport = transport;
        this.holograms = holograms;
        this.lines = lines;
        this.interceptor = PacketInterceptor.start(plugin, this::onUse);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_PERIOD, TICK_PERIOD);
    }

    public static NpcService start(Plugin plugin, Function<Npc, List<String>> lines) {
        return new NpcService(plugin, PacketNpcTransport.instance(), TextHolograms.standard(), lines);
    }

    public void onClick(BiConsumer<Player, Npc> handler) {
        this.clickHandler = handler;
    }

    public void put(Npc npc) {
        remove(npc.id());
        npcs.put(npc.id(), new Placed(npc, transport.create(npc)));
    }

    public void remove(String id) {
        Placed removed = npcs.remove(id);
        if (removed == null) {
            return;
        }
        for (Map.Entry<UUID, Map<String, Seen>> entry : viewers.entrySet()) {
            Seen seen = entry.getValue().remove(id);
            Player viewer = plugin.getServer().getPlayer(entry.getKey());
            if (seen != null && viewer != null) {
                hide(viewer, removed, seen);
            }
        }
    }

    public Optional<Npc> find(String id) {
        return Optional.ofNullable(npcs.get(id)).map(Placed::npc);
    }

    public Collection<Npc> all() {
        return npcs.values().stream().map(Placed::npc).toList();
    }

    public void stop() {
        interceptor.ifPresent(PacketInterceptor::stop);
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            Map<String, Seen> seen = viewers.remove(viewer.getUniqueId());
            if (seen != null) {
                seen.forEach((id, view) -> Optional.ofNullable(npcs.get(id)).ifPresent(placed -> hide(viewer, placed, view)));
            }
        }
    }

    private void tick() {
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            Map<String, Seen> seen = viewers.computeIfAbsent(viewer.getUniqueId(), ignored -> new ConcurrentHashMap<>());
            for (Placed placed : npcs.values()) {
                Seen view = seen.get(placed.npc().id());
                switch (NpcRange.decide(view != null, distanceSquared(viewer, placed.npc().location()))) {
                    case SHOW -> seen.put(placed.npc().id(), show(viewer, placed));
                    case HIDE -> {
                        seen.remove(placed.npc().id());
                        hide(viewer, placed, view);
                    }
                    case KEEP -> {
                        if (view != null) {
                            refreshHologram(viewer, placed, view);
                        }
                    }
                }
            }
        }
    }

    private Seen show(Player viewer, Placed placed) {
        hideName(viewer.getScoreboard(), placed.npc().profileName());
        placed.body().show(viewer);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> placed.body().leaveTabList(viewer), TAB_LIST_DELAY_TICKS);
        List<String> text = currentLines(placed.npc());
        return new Seen(text, showHologram(viewer, placed.npc(), text));
    }

    private void hide(Player viewer, Placed placed, Seen seen) {
        placed.body().hide(viewer);
        if (seen != null) {
            seen.hologram().remove();
        }
    }

    private void refreshHologram(Player viewer, Placed placed, Seen view) {
        List<String> text = currentLines(placed.npc());
        if (text.equals(view.lines())) {
            return;
        }
        view.hologram().remove();
        viewers.get(viewer.getUniqueId()).put(placed.npc().id(), new Seen(text, showHologram(viewer, placed.npc(), text)));
    }

    private SpawnedHologram showHologram(Player viewer, Npc npc, List<String> text) {
        if (text.isEmpty()) {
            return SpawnedHologram.NONE;
        }
        Location top = npc.location().clone().add(0, HOLOGRAM_ABOVE_HEAD + holograms.height(text.size()), 0);
        return holograms.show(viewer, top, text);
    }

    private List<String> currentLines(Npc npc) {
        return lines.apply(npc).stream().map(Colors::translate).toList();
    }

    private void onUse(Player player, int entityId, EntityUse use) {
        if (use == EntityUse.INTERACT_AT) {
            return;
        }
        for (Placed placed : npcs.values()) {
            if (placed.body().entityId() == entityId) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        clickHandler.accept(player, placed.npc());
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

    private static void hideName(Scoreboard scoreboard, String profileName) {
        Team team = scoreboard.getTeam(HIDDEN_NAMES_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(HIDDEN_NAMES_TEAM);
            team.setNameTagVisibility(NameTagVisibility.NEVER);
        }
        if (!team.hasEntry(profileName)) {
            team.addEntry(profileName);
        }
    }

    private static double distanceSquared(Player viewer, Location npc) {
        Location at = viewer.getLocation();
        if (npc.getWorld() == null || !npc.getWorld().equals(at.getWorld())) {
            return Double.POSITIVE_INFINITY;
        }
        return at.distanceSquared(npc);
    }

    private record Placed(Npc npc, NpcBody body) {
    }

    private record Seen(List<String> lines, SpawnedHologram hologram) {
    }
}

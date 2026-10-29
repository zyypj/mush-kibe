package br.com.muxe.core.bukkit.bossbar;

import br.com.muxe.core.bukkit.nms.PacketBossBarTransport;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public final class BossBars implements Listener {

    public static final long FOLLOW_PERIOD_TICKS = 2L;

    private final Plugin plugin;
    private final PacketBossBarTransport transport;
    private final Map<UUID, Shown> bars = new ConcurrentHashMap<>();

    public BossBars(Plugin plugin, PacketBossBarTransport transport) {
        this.plugin = plugin;
        this.transport = transport;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::follow, FOLLOW_PERIOD_TICKS, FOLLOW_PERIOD_TICKS);
    }

    public static BossBars start(Plugin plugin) {
        return new BossBars(plugin, PacketBossBarTransport.instance());
    }

    public void show(Player player, String text) {
        Shown shown = bars.get(player.getUniqueId());
        if (shown == null) {
            spawn(player, text);
        } else if (!shown.text.equals(text)) {
            shown.text = text;
            shown.bar.rename(text);
        }
    }

    public void hide(Player player) {
        Shown shown = bars.remove(player.getUniqueId());
        if (shown != null) {
            shown.bar.remove();
        }
    }

    private void spawn(Player player, String text) {
        Location eyes = player.getEyeLocation();
        SpawnedBossBar bar = transport.spawn(player, BossBarPlacement.inFront(eyes, BossBarPlacement.DISTANCE), text);
        bars.put(player.getUniqueId(), new Shown(bar, text, eyes));
    }

    private void follow() {
        for (Map.Entry<UUID, Shown> entry : bars.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null) {
                bars.remove(entry.getKey());
                continue;
            }
            Shown shown = entry.getValue();
            Location eyes = player.getEyeLocation();
            if (BossBarPlacement.needsMove(shown.carriedFor, eyes)) {
                shown.bar.move(BossBarPlacement.inFront(eyes, BossBarPlacement.DISTANCE));
                shown.carriedFor = eyes;
            }
        }
    }

    private void respawnLater(Player player) {
        Shown shown = bars.remove(player.getUniqueId());
        if (shown == null) {
            return;
        }
        shown.bar.remove();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !bars.containsKey(player.getUniqueId())) {
                spawn(player, shown.text);
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bars.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        respawnLater(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        respawnLater(event.getPlayer());
    }

    private static final class Shown {
        private final SpawnedBossBar bar;
        private String text;
        private Location carriedFor;

        private Shown(SpawnedBossBar bar, String text, Location carriedFor) {
            this.bar = bar;
            this.text = text;
            this.carriedFor = carriedFor;
        }
    }
}

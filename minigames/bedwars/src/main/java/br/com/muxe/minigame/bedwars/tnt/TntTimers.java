package br.com.muxe.minigame.bedwars.tnt;

import br.com.muxe.core.bukkit.hologram.HologramPixel;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.bedwars.preferences.PlayerPreferences;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;

public final class TntTimers {

    private static final double MOVED_SQUARED = 0.0025;

    private final TntTimerSettings settings;
    private final Localized<TntTimerSettings> localized;
    private final LanguageLookup languages;
    private final PlayerPreferences preferences;
    private final Map<UUID, Timer> timers = new HashMap<>();

    private TntTimers(Localized<TntTimerSettings> localized, LanguageLookup languages, PlayerPreferences preferences) {
        this.settings = localized.standard();
        this.localized = localized;
        this.languages = languages;
        this.preferences = preferences;
    }

    public static TntTimers start(Plugin plugin, Localized<TntTimerSettings> settings, LanguageLookup languages, PlayerPreferences preferences) {
        TntTimers started = new TntTimers(settings, languages, preferences);
        plugin.getServer().getScheduler().runTaskTimer(plugin, started::update, started.settings.updateTicks(), started.settings.updateTicks());
        return started;
    }

    public void track(TNTPrimed tnt) {
        timers.put(tnt.getUniqueId(), new Timer(tnt, Math.max(1, tnt.getFuseTicks())));
    }

    private void update() {
        Iterator<Timer> iterator = timers.values().iterator();
        while (iterator.hasNext()) {
            Timer timer = iterator.next();
            if (!timer.tnt.isValid() || timer.tnt.isDead()) {
                timer.clear();
                iterator.remove();
                continue;
            }
            timer.update();
        }
    }

    private final class Timer {

        private final TNTPrimed tnt;
        private final int fuseTicks;
        private final Map<UUID, Shown> viewers = new HashMap<>();

        private Timer(TNTPrimed tnt, int fuseTicks) {
            this.tnt = tnt;
            this.fuseTicks = fuseTicks;
        }

        private void update() {
            Location at = tnt.getLocation().add(0, settings.height(), 0);
            double range = settings.viewDistance() * settings.viewDistance();
            Map<UUID, Player> wanted = new HashMap<>();
            for (Player player : tnt.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(at) <= range && preferences.of(player.getUniqueId()).tntTimer()) {
                    wanted.put(player.getUniqueId(), player);
                }
            }
            viewers.entrySet().removeIf(entry -> {
                if (wanted.containsKey(entry.getKey())) {
                    return false;
                }
                entry.getValue().hologram().remove();
                return true;
            });
            wanted.forEach((id, player) -> {
                String text = Colors.translate(localized.in(languages.of(id)).text(tnt.getFuseTicks(), fuseTicks));
                Shown shown = viewers.get(id);
                boolean renamed = shown != null && shown.at().distanceSquared(at) <= MOVED_SQUARED
                    && (shown.text().equals(text) || shown.hologram().rename(List.of(text)));
                if (renamed) {
                    viewers.put(id, new Shown(shown.hologram(), shown.at(), text));
                } else {
                    if (shown != null) {
                        shown.hologram().remove();
                    }
                    viewers.put(id, new Shown(PacketHologramTransport.instance().spawn(player, List.of(new HologramPixel(at, text))), at, text));
                }
            });
        }

        private void clear() {
            viewers.values().forEach(shown -> shown.hologram().remove());
            viewers.clear();
        }
    }

    private record Shown(SpawnedHologram hologram, Location at, String text) {
    }
}

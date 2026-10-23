package br.com.muxe.auth.challenge;

import br.com.muxe.core.bukkit.effect.ParticleText;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ChallengeRenderer implements Runnable {

    public static final long PERIOD_TICKS = 4L;

    private final ChallengeTracker tracker;
    private final ParticleText text;
    private final Map<UUID, Location> anchors = new ConcurrentHashMap<>();

    public ChallengeRenderer(ChallengeTracker tracker, ParticleText text) {
        this.tracker = tracker;
        this.text = text;
    }

    public void track(UUID playerId, Location anchor) {
        anchors.put(playerId, anchor.clone());
    }

    public void untrack(UUID playerId) {
        anchors.remove(playerId);
    }

    @Override
    public void run() {
        anchors.forEach((playerId, anchor) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                anchors.remove(playerId);
                return;
            }
            tracker.code(playerId).ifPresent(code -> text.show(player, anchor, code));
        });
    }
}

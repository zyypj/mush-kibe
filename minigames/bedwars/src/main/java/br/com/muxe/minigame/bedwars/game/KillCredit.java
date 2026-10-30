package br.com.muxe.minigame.bedwars.game;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class KillCredit {

    public static final Duration WINDOW = Duration.ofSeconds(15);

    private final Duration window;
    private final Map<UUID, Hit> lastHits = new HashMap<>();

    public KillCredit() {
        this(WINDOW);
    }

    public KillCredit(Duration window) {
        this.window = window;
    }

    public void hit(UUID victim, UUID attacker, Instant at) {
        if (!victim.equals(attacker)) {
            lastHits.put(victim, new Hit(attacker, at));
        }
    }

    public Optional<UUID> lastHitter(UUID victim, Instant now) {
        Hit hit = lastHits.get(victim);
        if (hit == null || hit.at().plus(window).isBefore(now)) {
            return Optional.empty();
        }
        return Optional.of(hit.attacker());
    }

    public Optional<UUID> killer(UUID victim, Instant now, boolean finalKill, Optional<UUID> bedDestroyer) {
        Optional<UUID> hitter = lastHitter(victim, now);
        if (hitter.isPresent() || !finalKill) {
            return hitter;
        }
        return bedDestroyer.filter(destroyer -> !destroyer.equals(victim));
    }

    public void forget(UUID victim) {
        lastHits.remove(victim);
    }

    private record Hit(UUID attacker, Instant at) {
    }
}

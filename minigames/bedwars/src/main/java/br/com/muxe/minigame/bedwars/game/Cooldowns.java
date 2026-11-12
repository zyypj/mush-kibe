package br.com.muxe.minigame.bedwars.game;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class Cooldowns {

    private final Duration length;
    private final Map<UUID, Instant> until = new HashMap<>();

    public Cooldowns(Duration length) {
        this.length = length;
    }

    public Optional<Duration> remaining(UUID player, Instant now) {
        Instant end = until.get(player);
        if (end == null || !now.isBefore(end)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, end));
    }

    public void start(UUID player, Instant now) {
        until.put(player, now.plus(length));
    }

    public void forget(UUID player) {
        until.remove(player);
    }

    public static String seconds(Duration remaining, String decimalSeparator) {
        long tenths = (remaining.toMillis() + 99) / 100;
        return tenths / 10 + decimalSeparator + tenths % 10;
    }
}

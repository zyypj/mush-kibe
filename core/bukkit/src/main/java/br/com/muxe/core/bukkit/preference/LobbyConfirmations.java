package br.com.muxe.core.bukkit.preference;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LobbyConfirmations {

    public static final Duration WINDOW = Duration.ofSeconds(5);

    private final Map<UUID, Instant> asked = new ConcurrentHashMap<>();

    public boolean confirmed(UUID player, Instant now) {
        Instant first = asked.remove(player);
        if (first != null && !now.isAfter(first.plus(WINDOW))) {
            return true;
        }
        asked.put(player, now);
        return false;
    }

    public void forget(UUID player) {
        asked.remove(player);
    }
}

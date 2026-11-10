package br.com.muxe.minigame.api.rescue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StrandedPlayers {

    public static final Duration GRACE = Duration.ofSeconds(5);
    public static final Duration RETRY = Duration.ofSeconds(15);

    private final Map<UUID, Instant> nextRescue = new HashMap<>();

    public List<UUID> toRescue(Collection<UUID> outside, Instant now) {
        nextRescue.keySet().retainAll(Set.copyOf(outside));
        List<UUID> rescued = new ArrayList<>();
        for (UUID id : outside) {
            Instant due = nextRescue.computeIfAbsent(id, ignored -> now.plus(GRACE));
            if (!now.isBefore(due)) {
                rescued.add(id);
                nextRescue.put(id, now.plus(RETRY));
            }
        }
        return rescued;
    }

    public void rescued(UUID id, Instant now) {
        nextRescue.put(id, now.plus(RETRY));
    }
}

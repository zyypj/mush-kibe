package br.com.muxe.core.common.cooldown;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInterval {

    private final Clock clock;
    private final Map<UUID, Instant> lastMessage = new ConcurrentHashMap<>();

    public ChatInterval(Clock clock) {
        this.clock = clock;
    }

    public Optional<Duration> speak(UUID player, Duration interval) {
        if (interval.isNegative() || interval.isZero()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        Duration[] wait = new Duration[1];
        lastMessage.compute(player, (id, previous) -> {
            if (previous != null && now.isBefore(previous.plus(interval))) {
                wait[0] = Duration.between(now, previous.plus(interval));
                return previous;
            }
            return now;
        });
        return Optional.ofNullable(wait[0]);
    }

    public void forget(UUID player) {
        lastMessage.remove(player);
    }
}

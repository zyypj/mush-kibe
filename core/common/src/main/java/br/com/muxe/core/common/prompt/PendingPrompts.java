package br.com.muxe.core.common.prompt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingPrompts<T> {

    private final Clock clock;
    private final Map<UUID, Pending<T>> pending = new ConcurrentHashMap<>();

    public PendingPrompts(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void open(UUID id, Duration timeout, T prompt) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Prompts need a positive timeout, got " + timeout);
        }
        pending.put(id, new Pending<>(Objects.requireNonNull(prompt, "prompt"), clock.instant().plus(timeout)));
    }

    public boolean isWaiting(UUID id) {
        Pending<T> current = pending.get(id);
        return current != null && !current.expiredAt(clock.instant());
    }

    public Optional<T> answer(UUID id) {
        Pending<T> current = pending.get(id);
        if (current == null || current.expiredAt(clock.instant())) {
            return Optional.empty();
        }
        return pending.remove(id, current) ? Optional.of(current.prompt()) : Optional.empty();
    }

    public Optional<T> cancel(UUID id) {
        return Optional.ofNullable(pending.remove(id)).map(Pending::prompt);
    }

    public List<T> expire() {
        Instant now = clock.instant();
        List<T> expired = new ArrayList<>();
        pending.forEach((id, current) -> {
            if (current.expiredAt(now) && pending.remove(id, current)) {
                expired.add(current.prompt());
            }
        });
        return expired;
    }

    private record Pending<T>(T prompt, Instant deadline) {

        private boolean expiredAt(Instant now) {
            return !now.isBefore(deadline);
        }
    }
}

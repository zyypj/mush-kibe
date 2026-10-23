package br.com.muxe.auth.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LoginAttemptLimiter {

    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final Map<UUID, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(Clock clock, int maxAttempts, Duration window) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    public int recordFailure(UUID playerId) {
        Deque<Instant> attempts = failures.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(clock.instant());
            return Math.max(0, maxAttempts - attempts.size());
        }
    }

    public boolean isBlocked(UUID playerId) {
        Deque<Instant> attempts = failures.get(playerId);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            prune(attempts);
            return attempts.size() >= maxAttempts;
        }
    }

    public void reset(UUID playerId) {
        failures.remove(playerId);
    }

    private void prune(Deque<Instant> attempts) {
        Instant oldestAllowed = clock.instant().minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(oldestAllowed)) {
            attempts.removeFirst();
        }
    }
}

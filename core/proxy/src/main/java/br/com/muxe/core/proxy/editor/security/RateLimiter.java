package br.com.muxe.core.proxy.editor.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limit, Duration window, Clock clock) {
        if (limit < 1 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("A rate limit needs a positive limit and window");
        }
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(entry -> entry.isOver(now));
        }
        Window updated = windows.compute(key, (ignored, current) -> current == null || current.isOver(now)
            ? new Window(now.plus(window), 1)
            : new Window(current.endsAt(), current.count() + 1));
        return updated.count() <= limit;
    }

    public boolean isBlocked(String key) {
        Window current = windows.get(key);
        return current != null && !current.isOver(clock.instant()) && current.count() >= limit;
    }

    private record Window(Instant endsAt, int count) {

        boolean isOver(Instant now) {
            return !now.isBefore(endsAt);
        }
    }
}

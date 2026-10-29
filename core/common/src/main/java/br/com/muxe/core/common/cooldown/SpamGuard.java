package br.com.muxe.core.common.cooldown;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpamGuard {

    private final Clock clock;
    private final int maxUses;
    private final Duration window;
    private final Duration cooldown;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    public SpamGuard(Clock clock, int maxUses, Duration window, Duration cooldown) {
        if (maxUses < 1) {
            throw new IllegalArgumentException("maxUses must be positive");
        }
        if (window.isNegative() || window.isZero() || cooldown.isNegative()) {
            throw new IllegalArgumentException("window must be positive and cooldown cannot be negative");
        }
        this.clock = clock;
        this.maxUses = maxUses;
        this.window = window;
        this.cooldown = cooldown;
    }

    public SpamDecision use(UUID key) {
        Instant now = clock.instant();
        State state = states.computeIfAbsent(key, ignored -> new State());
        synchronized (state) {
            if (state.blockedUntil != null && now.isBefore(state.blockedUntil)) {
                return SpamDecision.block(Duration.between(now, state.blockedUntil));
            }
            Instant windowStart = now.minus(window);
            while (!state.uses.isEmpty() && !state.uses.peekFirst().isAfter(windowStart)) {
                state.uses.pollFirst();
            }
            state.uses.addLast(now);
            if (state.uses.size() >= maxUses) {
                state.uses.clear();
                state.blockedUntil = now.plus(cooldown);
            }
            return SpamDecision.allow();
        }
    }

    public void forget(UUID key) {
        states.remove(key);
    }

    private static final class State {
        private final Deque<Instant> uses = new ArrayDeque<>();
        private Instant blockedUntil;
    }
}

package br.com.muxe.minigame.engine.arena;

import br.com.muxe.minigame.engine.match.IllegalTransitionException;
import java.time.Duration;
import java.util.Objects;

public final class ArenaInstance {

    public static final int DEFAULT_MAX_FAILURES = 5;

    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    private final String id;
    private final String templateId;
    private final int maxFailures;
    private ArenaState state = ArenaState.AVAILABLE;
    private int consecutiveFailures;

    public ArenaInstance(String id, String templateId, int maxFailures) {
        this.id = Objects.requireNonNull(id, "id");
        this.templateId = Objects.requireNonNull(templateId, "templateId");
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be positive");
        }
        this.maxFailures = maxFailures;
    }

    public String id() {
        return id;
    }

    public String templateId() {
        return templateId;
    }

    public ArenaState state() {
        return state;
    }

    public int consecutiveFailures() {
        return consecutiveFailures;
    }

    public void reserve() {
        move(ArenaState.AVAILABLE, ArenaState.RESERVED);
    }

    public void startUsing() {
        move(ArenaState.RESERVED, ArenaState.IN_USE);
    }

    public void release() {
        if (state != ArenaState.IN_USE && state != ArenaState.RESERVED) {
            throw new IllegalTransitionException("Arena " + id, state, ArenaState.RESETTING);
        }
        state = ArenaState.RESETTING;
    }

    public void resetSucceeded() {
        move(ArenaState.RESETTING, ArenaState.AVAILABLE);
        consecutiveFailures = 0;
    }

    public void resetFailed(boolean permanent) {
        if (state != ArenaState.RESETTING) {
            throw new IllegalTransitionException("Arena " + id, state, ArenaState.RESETTING);
        }
        consecutiveFailures++;
        if (permanent || consecutiveFailures >= maxFailures) {
            state = ArenaState.DISABLED;
        }
    }

    public void enable() {
        move(ArenaState.DISABLED, ArenaState.RESETTING);
        consecutiveFailures = 0;
    }

    public Duration nextRetryDelay(Duration base) {
        if (consecutiveFailures == 0) {
            return Duration.ZERO;
        }
        Duration delay = base.multipliedBy(1L << Math.min(consecutiveFailures - 1, 16));
        return delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay;
    }

    private void move(ArenaState expected, ArenaState next) {
        if (state != expected) {
            throw new IllegalTransitionException("Arena " + id, state, next);
        }
        state = next;
    }
}

package br.com.muxe.core.bukkit.config;

import java.time.Duration;

public final class Ticks {

    public static final long MILLIS_PER_TICK = 50;

    private Ticks() {
    }

    public static long of(Duration duration) {
        return Math.max(1, duration.toMillis() / MILLIS_PER_TICK);
    }
}

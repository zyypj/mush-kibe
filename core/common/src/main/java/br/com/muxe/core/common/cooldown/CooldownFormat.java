package br.com.muxe.core.common.cooldown;

import java.time.Duration;

public final class CooldownFormat {

    private static final long MILLIS_PER_TENTH = 100;

    private CooldownFormat() {
    }

    public static String seconds(Duration remaining) {
        long millis = Math.max(0, remaining.toMillis());
        long tenths = Math.max(1, (millis + MILLIS_PER_TENTH - 1) / MILLIS_PER_TENTH);
        return (tenths / 10) + "," + (tenths % 10);
    }
}

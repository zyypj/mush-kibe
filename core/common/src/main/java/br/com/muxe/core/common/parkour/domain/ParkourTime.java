package br.com.muxe.core.common.parkour.domain;

import java.time.Duration;
import java.util.Locale;

public final class ParkourTime {

    private ParkourTime() {
    }

    public static String format(Duration time) {
        long millis = Math.max(0, time.toMillis());
        long hours = millis / 3_600_000;
        long minutes = millis / 60_000 % 60;
        long seconds = millis / 1_000 % 60;
        long rest = millis % 1_000;
        StringBuilder text = new StringBuilder();
        if (hours > 0) {
            text.append(hours).append("h ");
        }
        if (hours > 0 || minutes > 0) {
            text.append(minutes).append("m ");
        }
        return text.append(String.format(Locale.ROOT, "%d,%03ds", seconds, rest)).toString();
    }
}

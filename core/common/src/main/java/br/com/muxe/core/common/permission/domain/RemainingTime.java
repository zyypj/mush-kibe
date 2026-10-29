package br.com.muxe.core.common.permission.domain;

import java.time.Duration;

public final class RemainingTime {

    private RemainingTime() {
    }

    public static String format(Duration remaining) {
        long seconds = Math.max(0, remaining.toSeconds());
        long days = seconds / 86_400;
        long hours = seconds % 86_400 / 3_600;
        long minutes = seconds % 3_600 / 60;
        long rest = seconds % 60;
        if (days > 0) {
            return pair(days, "d", hours, "h");
        }
        if (hours > 0) {
            return pair(hours, "h", minutes, "m");
        }
        if (minutes > 0) {
            return pair(minutes, "m", rest, "s");
        }
        return rest + "s";
    }

    private static String pair(long first, String firstUnit, long second, String secondUnit) {
        return second == 0 ? first + firstUnit : first + firstUnit + " " + second + secondUnit;
    }
}

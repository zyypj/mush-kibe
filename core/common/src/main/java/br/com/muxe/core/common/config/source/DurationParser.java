package br.com.muxe.core.common.config.source;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern FORMAT = Pattern.compile("(\\d+)\\s*(ms|s|m|h|d)");

    private DurationParser() {
    }

    public static Duration parse(String value) {
        Matcher matcher = FORMAT.matcher(value.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Durations look like 500ms, 5s, 2m, 1h or 1d, got: " + value);
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalStateException("Unreachable unit " + matcher.group(2));
        };
    }
}

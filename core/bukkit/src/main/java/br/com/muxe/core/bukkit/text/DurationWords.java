package br.com.muxe.core.bukkit.text;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record DurationWords(String day, String days, String hour, String hours, String minute, String minutes, String second, String seconds) {

    public static final DurationWords PORTUGUESE = new DurationWords("dia", "dias", "hora", "horas", "minuto", "minutos", "segundo", "segundos");
    public static final String SECTION = "time-units";

    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_HOUR = 3_600;
    private static final long SECONDS_PER_DAY = 86_400;

    public DurationWords {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(days, "days");
        Objects.requireNonNull(hour, "hour");
        Objects.requireNonNull(hours, "hours");
        Objects.requireNonNull(minute, "minute");
        Objects.requireNonNull(minutes, "minutes");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(seconds, "seconds");
    }

    public static DurationWords from(ConfigurationSection root) {
        ConfigurationSection section = root == null ? null : root.getConfigurationSection(SECTION);
        if (section == null) {
            return PORTUGUESE;
        }
        return new DurationWords(section.getString("day", PORTUGUESE.day), section.getString("days", PORTUGUESE.days), section.getString("hour", PORTUGUESE.hour),
            section.getString("hours", PORTUGUESE.hours), section.getString("minute", PORTUGUESE.minute), section.getString("minutes", PORTUGUESE.minutes),
            section.getString("second", PORTUGUESE.second), section.getString("seconds", PORTUGUESE.seconds));
    }

    public String full(Duration duration) {
        long total = Math.max(0, duration.toSeconds());
        List<String> parts = new ArrayList<>();
        add(parts, total / SECONDS_PER_DAY, day, days);
        add(parts, total % SECONDS_PER_DAY / SECONDS_PER_HOUR, hour, hours);
        add(parts, total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE, minute, minutes);
        add(parts, total % SECONDS_PER_MINUTE, second, seconds);
        return parts.isEmpty() ? unit(0, second, seconds) : String.join(" ", parts);
    }

    public String largest(Duration duration) {
        long total = Math.max(0, duration.toSeconds());
        if (total >= SECONDS_PER_DAY) {
            return unit(total / SECONDS_PER_DAY, day, days);
        }
        if (total >= SECONDS_PER_HOUR) {
            return unit(total / SECONDS_PER_HOUR, hour, hours);
        }
        if (total >= SECONDS_PER_MINUTE) {
            return unit(total / SECONDS_PER_MINUTE, minute, minutes);
        }
        return unit(total, second, seconds);
    }

    private static void add(List<String> parts, long amount, String one, String many) {
        if (amount > 0) {
            parts.add(unit(amount, one, many));
        }
    }

    private static String unit(long amount, String one, String many) {
        return amount + " " + (amount == 1 ? one : many);
    }
}

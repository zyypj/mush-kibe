package br.com.muxe.core.common.schedule;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record WeeklySchedule(ZoneId zone, List<LocalTime> weekdays, List<LocalTime> weekends, Duration window) {

    public WeeklySchedule {
        Objects.requireNonNull(zone, "zone");
        weekdays = List.copyOf(weekdays);
        weekends = List.copyOf(weekends);
        Objects.requireNonNull(window, "window");
        if (window.isNegative() || window.isZero() || window.compareTo(Duration.ofDays(1)) >= 0) {
            throw new IllegalArgumentException("A schedule window lasts more than zero and less than a day, got: " + window);
        }
    }

    public boolean isOpen(Instant now) {
        return openedAt(now).isPresent();
    }

    public Optional<Instant> openedAt(Instant now) {
        ZonedDateTime local = now.atZone(zone);
        for (LocalDate day : List.of(local.toLocalDate(), local.toLocalDate().minusDays(1))) {
            for (LocalTime time : timesOf(day.getDayOfWeek())) {
                Instant start = day.atTime(time).atZone(zone).toInstant();
                if (!now.isBefore(start) && now.isBefore(start.plus(window))) {
                    return Optional.of(start);
                }
            }
        }
        return Optional.empty();
    }

    public List<LocalTime> timesOf(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ? weekends : weekdays;
    }
}

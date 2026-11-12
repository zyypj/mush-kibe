package br.com.muxe.core.common.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyScheduleTest {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private final WeeklySchedule mega = new WeeklySchedule(BRASILIA, List.of(LocalTime.of(19, 0)),
        List.of(LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(19, 0)), Duration.ofMinutes(15));

    @Test
    void aWeekdayOpensOnlyAtSevenInTheEveningForTheWindow() {
        assertThat(mega.isOpen(brasilia("2026-11-12T18:59:59"))).isFalse();
        assertThat(mega.openedAt(brasilia("2026-11-12T19:00:00"))).contains(brasilia("2026-11-12T19:00:00"));
        assertThat(mega.openedAt(brasilia("2026-11-12T19:14:59"))).contains(brasilia("2026-11-12T19:00:00"));
        assertThat(mega.isOpen(brasilia("2026-11-12T19:15:00"))).isFalse();
        assertThat(mega.isOpen(brasilia("2026-11-12T15:05:00"))).isFalse();
    }

    @Test
    void theWeekendOpensAtThreeFiveAndSeven() {
        assertThat(mega.isOpen(brasilia("2026-11-14T15:05:00"))).isTrue();
        assertThat(mega.isOpen(brasilia("2026-11-15T17:10:00"))).isTrue();
        assertThat(mega.isOpen(brasilia("2026-11-15T19:00:00"))).isTrue();
        assertThat(mega.isOpen(brasilia("2026-11-15T16:00:00"))).isFalse();
    }

    @Test
    void aWindowThatCrossesMidnightStaysOpenOnTheNextDay() {
        WeeklySchedule late = new WeeklySchedule(BRASILIA, List.of(LocalTime.of(23, 50)), List.of(), Duration.ofMinutes(20));

        assertThat(late.openedAt(brasilia("2026-11-13T00:05:00"))).contains(brasilia("2026-11-12T23:50:00"));
        assertThat(late.isOpen(brasilia("2026-11-13T00:10:00"))).isFalse();
    }

    @Test
    void aWindowMustBePositiveAndShorterThanADay() {
        assertThatThrownBy(() -> new WeeklySchedule(BRASILIA, List.of(), List.of(), Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WeeklySchedule(BRASILIA, List.of(), List.of(), Duration.ofDays(1))).isInstanceOf(IllegalArgumentException.class);
    }

    private static Instant brasilia(String local) {
        return LocalDateTime.parse(local).atZone(BRASILIA).toInstant();
    }
}

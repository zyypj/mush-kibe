package br.com.muxe.core.common.parkour.domain;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record ParkourOutcome(Duration time, Optional<Duration> previous, boolean newRecord) {

    public ParkourOutcome {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(previous, "previous");
    }

    public Duration best() {
        return newRecord ? time : previous.orElse(time);
    }
}

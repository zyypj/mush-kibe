package br.com.muxe.core.common.parkour.domain;

import java.time.Instant;
import java.util.Objects;

public record ParkourRun(Instant startedAt, int reached) {

    public ParkourRun {
        Objects.requireNonNull(startedAt, "startedAt");
        if (reached < 0) {
            throw new IllegalArgumentException("Reached checkpoints cannot be negative");
        }
    }

    public ParkourRun reaching(int checkpoints) {
        return new ParkourRun(startedAt, checkpoints);
    }
}

package br.com.muxe.core.common.parkour.domain;

import br.com.muxe.core.common.placement.domain.Position;
import java.time.Duration;
import java.util.Optional;

public sealed interface ParkourStep {

    record Started() implements ParkourStep {
    }

    record Restarted(Optional<Position> teleport) implements ParkourStep {
    }

    record CheckpointReached(int number, Duration elapsed) implements ParkourStep {
    }

    record ReturnedToCheckpoint(int number, Position position) implements ParkourStep {
    }

    record Finished(Duration elapsed) implements ParkourStep {
    }

    record Ignored() implements ParkourStep {
    }
}

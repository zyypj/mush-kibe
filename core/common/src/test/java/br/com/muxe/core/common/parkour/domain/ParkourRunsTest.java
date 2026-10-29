package br.com.muxe.core.common.parkour.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkourRunsTest {

    private static final Position START = new Position("lobby", 28.5, 34, -8.5, 0, 0);
    private static final Position FIRST = new Position("lobby", -36.5, 86, 31.5, 0, 0);
    private static final Position SECOND = new Position("lobby", -56.5, 87, -58.5, 0, 0);
    private static final Position END = new Position("lobby", 48.5, 118, -73.5, 0, 0);

    private final ParkourCourse course = new ParkourCourse(
        point(ParkourPoint.start(), START),
        List.of(point(ParkourPoint.checkpoint(2), SECOND), point(ParkourPoint.checkpoint(1), FIRST)),
        point(ParkourPoint.end(), END)
    );
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-27T21:00:00Z"));
    private final ParkourRuns runs = new ParkourRuns(clock);
    private final UUID player = UUID.randomUUID();

    @Test
    void steppingOnTheStartStartsAndAgainRestartsTheTimer() {
        assertThat(runs.stepOn(player, course, ParkourPoint.start())).isEqualTo(new ParkourStep.Started());
        clock.advance(Duration.ofSeconds(30));

        assertThat(runs.stepOn(player, course, ParkourPoint.start())).isEqualTo(new ParkourStep.Restarted(Optional.empty()));
        assertThat(runs.run(player)).contains(new ParkourRun(clock.instant(), 0));
    }

    @Test
    void checkpointsAreReachedInOrderWithTheTimeSinceTheStart() {
        runs.stepOn(player, course, ParkourPoint.start());
        clock.advance(Duration.ofMillis(82_704));

        assertThat(runs.stepOn(player, course, ParkourPoint.checkpoint(1))).isEqualTo(new ParkourStep.CheckpointReached(1, Duration.ofMillis(82_704)));
        clock.advance(Duration.ofSeconds(10));
        assertThat(runs.stepOn(player, course, ParkourPoint.checkpoint(2))).isEqualTo(new ParkourStep.CheckpointReached(2, Duration.ofMillis(92_704)));
    }

    @Test
    void skippingACheckpointDoesNothingUntilThePreviousOneIsReached() {
        runs.stepOn(player, course, ParkourPoint.start());

        assertThat(runs.stepOn(player, course, ParkourPoint.checkpoint(2))).isEqualTo(new ParkourStep.Ignored());
        assertThat(runs.stepOn(player, course, ParkourPoint.end())).isEqualTo(new ParkourStep.Ignored());
        assertThat(runs.run(player).map(ParkourRun::reached)).contains(0);

        runs.stepOn(player, course, ParkourPoint.checkpoint(1));
        assertThat(runs.stepOn(player, course, ParkourPoint.checkpoint(1))).isEqualTo(new ParkourStep.Ignored());
        assertThat(runs.stepOn(player, course, ParkourPoint.checkpoint(2))).isInstanceOf(ParkourStep.CheckpointReached.class);
    }

    @Test
    void theEndFinishesOnlyAfterEveryCheckpointAndEndsTheRun() {
        runs.stepOn(player, course, ParkourPoint.start());
        runs.stepOn(player, course, ParkourPoint.checkpoint(1));
        runs.stepOn(player, course, ParkourPoint.checkpoint(2));
        clock.advance(Duration.ofMillis(298_902));

        assertThat(runs.stepOn(player, course, ParkourPoint.end())).isEqualTo(new ParkourStep.Finished(Duration.ofMillis(298_902)));
        assertThat(runs.isRunning(player)).isFalse();
    }

    @Test
    void nothingHappensForAPlayerWhoDidNotStart() {
        assertThat(runs.stepOn(player, course, ParkourPoint.checkpoint(1))).isEqualTo(new ParkourStep.Ignored());
        assertThat(runs.stepOn(player, course, ParkourPoint.end())).isEqualTo(new ParkourStep.Ignored());
        assertThat(runs.backToCheckpoint(player, course)).isEqualTo(new ParkourStep.Ignored());
        assertThat(runs.restart(player, course)).isEqualTo(new ParkourStep.Ignored());
        assertThat(runs.stop(player)).isFalse();
    }

    @Test
    void goingBackBeforeTheFirstCheckpointRestartsAtTheStart() {
        runs.stepOn(player, course, ParkourPoint.start());
        clock.advance(Duration.ofSeconds(20));

        assertThat(runs.backToCheckpoint(player, course)).isEqualTo(new ParkourStep.Restarted(Optional.of(START)));
        assertThat(runs.run(player)).contains(new ParkourRun(clock.instant(), 0));
    }

    @Test
    void goingBackReturnsToTheLastCheckpointAndKeepsTheTimer() {
        runs.stepOn(player, course, ParkourPoint.start());
        Instant started = clock.instant();
        runs.stepOn(player, course, ParkourPoint.checkpoint(1));
        clock.advance(Duration.ofSeconds(20));

        assertThat(runs.backToCheckpoint(player, course)).isEqualTo(new ParkourStep.ReturnedToCheckpoint(1, FIRST));
        assertThat(runs.run(player)).contains(new ParkourRun(started, 1));
    }

    @Test
    void restartingSendsBackToTheStartAndForgetsTheCheckpoints() {
        runs.stepOn(player, course, ParkourPoint.start());
        runs.stepOn(player, course, ParkourPoint.checkpoint(1));
        clock.advance(Duration.ofSeconds(5));

        assertThat(runs.restart(player, course)).isEqualTo(new ParkourStep.Restarted(Optional.of(START)));
        assertThat(runs.run(player)).contains(new ParkourRun(clock.instant(), 0));
    }

    @Test
    void stoppingEndsTheRun() {
        runs.stepOn(player, course, ParkourPoint.start());

        assertThat(runs.stop(player)).isTrue();
        assertThat(runs.isRunning(player)).isFalse();
    }

    private static ParkourPointPlacement point(ParkourPoint point, Position position) {
        return new ParkourPointPlacement("lobby", point, position);
    }
}

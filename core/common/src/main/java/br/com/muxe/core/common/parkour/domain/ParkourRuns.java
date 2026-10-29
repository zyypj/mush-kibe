package br.com.muxe.core.common.parkour.domain;

import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParkourRuns {

    private final Clock clock;
    private final Map<UUID, ParkourRun> runs = new ConcurrentHashMap<>();

    public ParkourRuns(Clock clock) {
        this.clock = clock;
    }

    public ParkourStep stepOn(UUID player, ParkourCourse course, ParkourPoint point) {
        ParkourRun run = runs.get(player);
        return switch (point.type()) {
            case START -> {
                runs.put(player, fresh());
                yield run == null ? new ParkourStep.Started() : new ParkourStep.Restarted(Optional.empty());
            }
            case CHECKPOINT -> {
                int index = course.checkpointIndex(point);
                if (run == null || index != run.reached()) {
                    yield new ParkourStep.Ignored();
                }
                runs.put(player, run.reaching(index + 1));
                yield new ParkourStep.CheckpointReached(point.number(), elapsed(run));
            }
            case END -> {
                if (run == null || run.reached() != course.checkpoints().size()) {
                    yield new ParkourStep.Ignored();
                }
                runs.remove(player);
                yield new ParkourStep.Finished(elapsed(run));
            }
        };
    }

    public ParkourStep backToCheckpoint(UUID player, ParkourCourse course) {
        ParkourRun run = runs.get(player);
        if (run == null) {
            return new ParkourStep.Ignored();
        }
        if (run.reached() == 0 || run.reached() > course.checkpoints().size()) {
            return restart(player, course);
        }
        ParkourPointPlacement checkpoint = course.checkpoints().get(run.reached() - 1);
        return new ParkourStep.ReturnedToCheckpoint(checkpoint.point().number(), checkpoint.position());
    }

    public ParkourStep restart(UUID player, ParkourCourse course) {
        if (!runs.containsKey(player)) {
            return new ParkourStep.Ignored();
        }
        runs.put(player, fresh());
        return new ParkourStep.Restarted(Optional.of(course.start().position()));
    }

    public boolean stop(UUID player) {
        return runs.remove(player) != null;
    }

    public boolean isRunning(UUID player) {
        return runs.containsKey(player);
    }

    public Optional<ParkourRun> run(UUID player) {
        return Optional.ofNullable(runs.get(player));
    }

    private ParkourRun fresh() {
        return new ParkourRun(clock.instant(), 0);
    }

    private Duration elapsed(ParkourRun run) {
        return Duration.between(run.startedAt(), clock.instant());
    }
}

package br.com.muxe.core.common.parkour.domain;

import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import br.com.muxe.core.common.placement.domain.Position;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record ParkourCourse(ParkourPointPlacement start, List<ParkourPointPlacement> checkpoints, ParkourPointPlacement end) {

    public ParkourCourse {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        checkpoints = checkpoints.stream().sorted(Comparator.comparingInt(placed -> placed.point().number())).toList();
    }

    public static Optional<ParkourCourse> from(List<ParkourPointPlacement> points) {
        Optional<ParkourPointPlacement> start = find(points, ParkourPoint.Type.START);
        Optional<ParkourPointPlacement> end = find(points, ParkourPoint.Type.END);
        if (start.isEmpty() || end.isEmpty()) {
            return Optional.empty();
        }
        List<ParkourPointPlacement> checkpoints = points.stream().filter(placed -> placed.point().type() == ParkourPoint.Type.CHECKPOINT).toList();
        return Optional.of(new ParkourCourse(start.get(), checkpoints, end.get()));
    }

    public Optional<ParkourPointPlacement> at(String world, int blockX, int blockY, int blockZ) {
        return all().filter(placed -> sameBlock(placed.position(), world, blockX, blockY, blockZ)).findFirst();
    }

    public int checkpointIndex(ParkourPoint point) {
        for (int index = 0; index < checkpoints.size(); index++) {
            if (checkpoints.get(index).point().equals(point)) {
                return index;
            }
        }
        return -1;
    }

    public Stream<ParkourPointPlacement> all() {
        return Stream.concat(Stream.concat(Stream.of(start), checkpoints.stream()), Stream.of(end));
    }

    private static boolean sameBlock(Position position, String world, int blockX, int blockY, int blockZ) {
        return position.world().equals(world)
            && (int) Math.floor(position.x()) == blockX
            && (int) Math.floor(position.y()) == blockY
            && (int) Math.floor(position.z()) == blockZ;
    }

    private static Optional<ParkourPointPlacement> find(List<ParkourPointPlacement> points, ParkourPoint.Type type) {
        return points.stream().filter(placed -> placed.point().type() == type).findFirst();
    }
}

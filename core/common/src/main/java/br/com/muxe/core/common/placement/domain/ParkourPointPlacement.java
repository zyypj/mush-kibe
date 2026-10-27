package br.com.muxe.core.common.placement.domain;

import java.util.Objects;

public record ParkourPointPlacement(String group, ParkourPoint point, Position position) implements Placement {

    public ParkourPointPlacement {
        PlacementIds.requireGroup(group);
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(position, "position");
    }

    @Override
    public String id() {
        return point.id();
    }

    @Override
    public PlacementKind kind() {
        return PlacementKind.PARKOUR;
    }

    public ParkourPointPlacement movedTo(Position target) {
        return new ParkourPointPlacement(group, point, target);
    }
}

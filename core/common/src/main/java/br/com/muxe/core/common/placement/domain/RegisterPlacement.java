package br.com.muxe.core.common.placement.domain;

import java.util.Objects;
import java.util.Optional;

public record RegisterPlacement(String group, String id, Position position, Optional<Position> seller) implements Placement {

    public RegisterPlacement {
        PlacementIds.requireGroup(group);
        PlacementIds.require(id);
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(seller, "seller");
    }

    @Override
    public PlacementKind kind() {
        return PlacementKind.REGISTER;
    }

    public RegisterPlacement movedTo(Position target) {
        return new RegisterPlacement(group, id, target, seller);
    }

    public RegisterPlacement withSeller(Optional<Position> value) {
        return new RegisterPlacement(group, id, position, value);
    }
}

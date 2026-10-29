package br.com.muxe.core.common.placement.domain;

import java.util.Objects;

public record SocialHeadPlacement(String group, String id, Position position) implements Placement {

    public SocialHeadPlacement {
        PlacementIds.requireGroup(group);
        PlacementIds.require(id);
        Objects.requireNonNull(position, "position");
    }

    @Override
    public PlacementKind kind() {
        return PlacementKind.SOCIAL_HEAD;
    }

    public SocialHeadPlacement movedTo(Position target) {
        return new SocialHeadPlacement(group, id, target);
    }
}

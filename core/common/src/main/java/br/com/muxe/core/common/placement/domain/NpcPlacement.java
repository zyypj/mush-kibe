package br.com.muxe.core.common.placement.domain;

import java.util.Objects;
import java.util.Optional;

public record NpcPlacement(String group, String id, Position position, String name, NpcAction action, String skinName, Optional<SkinTexture> skin) implements Placement {

    public NpcPlacement {
        PlacementIds.requireGroup(group);
        PlacementIds.require(id);
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(skinName, "skinName");
        Objects.requireNonNull(skin, "skin");
    }

    @Override
    public PlacementKind kind() {
        return PlacementKind.NPC;
    }

    public NpcPlacement movedTo(Position target) {
        return new NpcPlacement(group, id, target, name, action, skinName, skin);
    }

    public NpcPlacement named(String value) {
        return new NpcPlacement(group, id, position, value, action, skinName, skin);
    }

    public NpcPlacement doing(NpcAction value) {
        return new NpcPlacement(group, id, position, name, value, skinName, skin);
    }

    public NpcPlacement wearing(String newSkinName, Optional<SkinTexture> newSkin) {
        return new NpcPlacement(group, id, position, name, action, newSkinName, newSkin);
    }
}

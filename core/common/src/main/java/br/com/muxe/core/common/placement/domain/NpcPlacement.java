package br.com.muxe.core.common.placement.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record NpcPlacement(
    String group,
    String id,
    Position position,
    String name,
    NpcAction action,
    String skinName,
    Optional<SkinTexture> skin,
    List<String> lines,
    Optional<HeldItem> hand
) implements Placement {

    public NpcPlacement {
        PlacementIds.requireGroup(group);
        PlacementIds.require(id);
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(skinName, "skinName");
        Objects.requireNonNull(skin, "skin");
        lines = List.copyOf(lines);
        Objects.requireNonNull(hand, "hand");
    }

    public NpcPlacement(String group, String id, Position position, String name, NpcAction action, String skinName, Optional<SkinTexture> skin) {
        this(group, id, position, name, action, skinName, skin, List.of(), Optional.empty());
    }

    @Override
    public PlacementKind kind() {
        return PlacementKind.NPC;
    }

    public boolean hasOwnLines() {
        return !lines.isEmpty();
    }

    public NpcPlacement movedTo(Position target) {
        return new NpcPlacement(group, id, target, name, action, skinName, skin, lines, hand);
    }

    public NpcPlacement named(String value) {
        return new NpcPlacement(group, id, position, value, action, skinName, skin, lines, hand);
    }

    public NpcPlacement doing(NpcAction value) {
        return new NpcPlacement(group, id, position, name, value, skinName, skin, lines, hand);
    }

    public NpcPlacement wearing(String newSkinName, Optional<SkinTexture> newSkin) {
        return new NpcPlacement(group, id, position, name, action, newSkinName, newSkin, lines, hand);
    }

    public NpcPlacement showing(List<String> newLines) {
        return new NpcPlacement(group, id, position, name, action, skinName, skin, newLines, hand);
    }

    public NpcPlacement holding(Optional<HeldItem> item) {
        return new NpcPlacement(group, id, position, name, action, skinName, skin, lines, item);
    }
}

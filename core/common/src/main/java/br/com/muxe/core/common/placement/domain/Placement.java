package br.com.muxe.core.common.placement.domain;

public sealed interface Placement permits NpcPlacement, HologramPlacement, RegisterPlacement, SocialHeadPlacement, ParkourPointPlacement {

    String group();

    String id();

    Position position();

    PlacementKind kind();
}

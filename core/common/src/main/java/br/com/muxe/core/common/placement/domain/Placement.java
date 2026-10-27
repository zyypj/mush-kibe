package br.com.muxe.core.common.placement.domain;

public sealed interface Placement permits NpcPlacement, HologramPlacement, RegisterPlacement {

    String group();

    String id();

    Position position();

    PlacementKind kind();
}

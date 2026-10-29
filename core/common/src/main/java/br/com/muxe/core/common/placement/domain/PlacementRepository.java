package br.com.muxe.core.common.placement.domain;

public interface PlacementRepository {

    Placements findByGroup(String group);

    void save(Placement placement);

    boolean remove(PlacementKind kind, String group, String id);
}

package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.PlacementRepository;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.placement.domain.SocialHeadPlacement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryPlacementRepository implements PlacementRepository {

    private final Map<String, Placement> placements = new LinkedHashMap<>();

    @Override
    public Placements findByGroup(String group) {
        List<NpcPlacement> npcs = new ArrayList<>();
        List<HologramPlacement> holograms = new ArrayList<>();
        List<RegisterPlacement> registers = new ArrayList<>();
        List<SocialHeadPlacement> socialHeads = new ArrayList<>();
        for (Placement placement : placements.values()) {
            if (!placement.group().equals(group)) {
                continue;
            }
            switch (placement) {
                case NpcPlacement npc -> npcs.add(npc);
                case HologramPlacement hologram -> holograms.add(hologram);
                case RegisterPlacement register -> registers.add(register);
                case SocialHeadPlacement head -> socialHeads.add(head);
            }
        }
        return new Placements(npcs, holograms, registers, socialHeads);
    }

    @Override
    public void save(Placement placement) {
        placements.put(key(placement.kind(), placement.group(), placement.id()), placement);
    }

    @Override
    public boolean remove(PlacementKind kind, String group, String id) {
        return placements.remove(key(kind, group, id)) != null;
    }

    private static String key(PlacementKind kind, String group, String id) {
        return kind + "/" + group + "/" + id;
    }
}

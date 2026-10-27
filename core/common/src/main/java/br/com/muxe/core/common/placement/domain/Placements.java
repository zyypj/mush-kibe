package br.com.muxe.core.common.placement.domain;

import java.util.List;
import java.util.Optional;

public record Placements(List<NpcPlacement> npcs, List<HologramPlacement> holograms, List<RegisterPlacement> registers, List<SocialHeadPlacement> socialHeads) {

    public static final Placements NONE = new Placements(List.of(), List.of(), List.of(), List.of());

    public Placements {
        npcs = List.copyOf(npcs);
        holograms = List.copyOf(holograms);
        registers = List.copyOf(registers);
        socialHeads = List.copyOf(socialHeads);
    }

    public Optional<NpcPlacement> npc(String id) {
        return npcs.stream().filter(npc -> npc.id().equals(id)).findFirst();
    }

    public Optional<HologramPlacement> hologram(String id) {
        return holograms.stream().filter(hologram -> hologram.id().equals(id)).findFirst();
    }

    public Optional<RegisterPlacement> register(String id) {
        return registers.stream().filter(register -> register.id().equals(id)).findFirst();
    }

    public Optional<SocialHeadPlacement> socialHead(String id) {
        return socialHeads.stream().filter(head -> head.id().equals(id)).findFirst();
    }
}

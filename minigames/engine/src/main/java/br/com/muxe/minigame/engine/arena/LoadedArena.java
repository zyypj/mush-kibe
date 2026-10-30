package br.com.muxe.minigame.engine.arena;

import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.world.map.MapTemplate;

public record LoadedArena(ArenaDraft arena, MapTemplate map) {

    public String name() {
        return arena.name();
    }

    public String mode() {
        return arena.mode();
    }

    public String displayName() {
        return arena.displayName();
    }
}

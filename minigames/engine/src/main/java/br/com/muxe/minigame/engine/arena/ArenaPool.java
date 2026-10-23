package br.com.muxe.minigame.engine.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ArenaPool {

    private final List<ArenaInstance> arenas = new ArrayList<>();

    public void add(ArenaInstance arena) {
        if (find(arena.id()).isPresent()) {
            throw new IllegalArgumentException("Arena " + arena.id() + " is already registered");
        }
        arenas.add(arena);
    }

    public Optional<ArenaInstance> reserve(String templateId) {
        for (ArenaInstance arena : arenas) {
            if (arena.templateId().equals(templateId) && arena.state() == ArenaState.AVAILABLE) {
                arena.reserve();
                return Optional.of(arena);
            }
        }
        return Optional.empty();
    }

    public Optional<ArenaInstance> find(String arenaId) {
        return arenas.stream().filter(arena -> arena.id().equals(arenaId)).findFirst();
    }

    public long count(ArenaState state) {
        return arenas.stream().filter(arena -> arena.state() == state).count();
    }

    public List<ArenaInstance> all() {
        return List.copyOf(arenas);
    }
}

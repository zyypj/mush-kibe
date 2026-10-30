package br.com.muxe.minigame.engine.world.map;

import java.util.Optional;

public interface ChunkSource {

    Optional<ChunkData> load(int x, int z);

    void save(ChunkData chunk);
}

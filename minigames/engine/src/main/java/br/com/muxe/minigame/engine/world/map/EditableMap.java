package br.com.muxe.minigame.engine.world.map;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EditableMap implements ChunkSource {

    private final Map<Long, byte[]> chunks;

    public EditableMap(MapTemplate start) {
        this.chunks = new LinkedHashMap<>(start.compressedChunks());
    }

    @Override
    public synchronized Optional<ChunkData> load(int x, int z) {
        return Optional.ofNullable(chunks.get(ChunkData.key(x, z))).map(ChunkCodec::decompress);
    }

    @Override
    public synchronized void save(ChunkData chunk) {
        if (chunk.isEmpty()) {
            chunks.remove(chunk.key());
        } else {
            chunks.put(chunk.key(), ChunkCodec.compress(chunk));
        }
    }

    public synchronized MapTemplate snapshot() {
        return MapTemplate.ofCompressed(chunks);
    }
}

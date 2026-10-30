package br.com.muxe.minigame.engine.world.map;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MapTemplate {

    private final Map<Long, byte[]> chunks;
    private final ChunkBounds bounds;

    private MapTemplate(Map<Long, byte[]> chunks) {
        this.chunks = Collections.unmodifiableMap(new LinkedHashMap<>(chunks));
        this.bounds = ChunkBounds.of(this.chunks.keySet());
    }

    public static MapTemplate empty() {
        return new MapTemplate(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    static MapTemplate ofCompressed(Map<Long, byte[]> chunks) {
        return new MapTemplate(chunks);
    }

    public Optional<ChunkData> chunk(int x, int z) {
        return Optional.ofNullable(chunks.get(ChunkData.key(x, z))).map(ChunkCodec::decompress);
    }

    public boolean has(int x, int z) {
        return chunks.containsKey(ChunkData.key(x, z));
    }

    public Set<Long> keys() {
        return chunks.keySet();
    }

    public int chunkCount() {
        return chunks.size();
    }

    public long compressedBytes() {
        long total = 0;
        for (byte[] chunk : chunks.values()) {
            total += chunk.length;
        }
        return total;
    }

    public ChunkBounds bounds() {
        return bounds;
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.chunks.putAll(chunks);
        return builder;
    }

    Map<Long, byte[]> compressedChunks() {
        return chunks;
    }

    public static final class Builder {

        private final Map<Long, byte[]> chunks = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder put(ChunkData chunk) {
            if (chunk.isEmpty()) {
                chunks.remove(chunk.key());
            } else {
                chunks.put(chunk.key(), ChunkCodec.compress(chunk));
            }
            return this;
        }

        public Builder remove(int x, int z) {
            chunks.remove(ChunkData.key(x, z));
            return this;
        }

        public MapTemplate build() {
            return new MapTemplate(chunks);
        }
    }
}

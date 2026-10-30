package br.com.muxe.minigame.engine.world.map;

import java.util.Collection;

public record ChunkBounds(int minX, int minZ, int maxX, int maxZ) {

    public static final ChunkBounds NONE = new ChunkBounds(0, 0, -1, -1);

    public static ChunkBounds of(Collection<Long> keys) {
        if (keys.isEmpty()) {
            return NONE;
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (long key : keys) {
            int x = ChunkData.keyX(key);
            int z = ChunkData.keyZ(key);
            minX = Math.min(minX, x);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxZ = Math.max(maxZ, z);
        }
        return new ChunkBounds(minX, minZ, maxX, maxZ);
    }

    public boolean isEmpty() {
        return maxX < minX || maxZ < minZ;
    }

    public int width() {
        return isEmpty() ? 0 : maxX - minX + 1;
    }

    public int depth() {
        return isEmpty() ? 0 : maxZ - minZ + 1;
    }
}

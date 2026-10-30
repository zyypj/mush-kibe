package br.com.muxe.minigame.engine.world.cell;

public record BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static final int MIN_Y = 0;
    public static final int MAX_Y = 255;

    public BlockBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("A box needs its minimum before its maximum");
        }
    }

    public static BlockBox of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new BlockBox(
            Math.min(x1, x2), Math.clamp(Math.min(y1, y2), MIN_Y, MAX_Y), Math.min(z1, z2),
            Math.max(x1, x2), Math.clamp(Math.max(y1, y2), MIN_Y, MAX_Y), Math.max(z1, z2)
        );
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public int minChunkX() {
        return minX >> 4;
    }

    public int maxChunkX() {
        return maxX >> 4;
    }

    public int minChunkZ() {
        return minZ >> 4;
    }

    public int maxChunkZ() {
        return maxZ >> 4;
    }

    public int minSection() {
        return minY >> 4;
    }

    public int maxSection() {
        return maxY >> 4;
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public int chunks() {
        return (maxChunkX() - minChunkX() + 1) * (maxChunkZ() - minChunkZ() + 1);
    }

    public Span inChunk(int chunkX, int chunkZ, int section) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int baseY = section << 4;
        return new Span(
            Math.max(minX, baseX) - baseX, Math.min(maxX, baseX + 15) - baseX,
            Math.max(minY, baseY) - baseY, Math.min(maxY, baseY + 15) - baseY,
            Math.max(minZ, baseZ) - baseZ, Math.min(maxZ, baseZ + 15) - baseZ
        );
    }

    public record Span(int fromX, int toX, int fromY, int toY, int fromZ, int toZ) {

        public boolean isEmpty() {
            return fromX > toX || fromY > toY || fromZ > toZ;
        }

        public int size() {
            return isEmpty() ? 0 : (toX - fromX + 1) * (toY - fromY + 1) * (toZ - fromZ + 1);
        }
    }
}

package br.com.muxe.minigame.engine.world.cell;

public record Cell(int index, int originChunkX, int originChunkZ, int sizeChunks) {

    public int offsetChunkX() {
        return originChunkX + sizeChunks / 2;
    }

    public int offsetChunkZ() {
        return originChunkZ + sizeChunks / 2;
    }

    public int offsetBlockX() {
        return offsetChunkX() * 16;
    }

    public int offsetBlockZ() {
        return offsetChunkZ() * 16;
    }

    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunkX >= originChunkX && chunkX < originChunkX + sizeChunks && chunkZ >= originChunkZ && chunkZ < originChunkZ + sizeChunks;
    }

    public boolean containsBlock(double x, double z) {
        return containsChunk(Math.floorDiv((int) Math.floor(x), 16), Math.floorDiv((int) Math.floor(z), 16));
    }

    public double toWorldX(double mapX) {
        return mapX + offsetBlockX();
    }

    public double toWorldZ(double mapZ) {
        return mapZ + offsetBlockZ();
    }

    public double toMapX(double worldX) {
        return worldX - offsetBlockX();
    }

    public double toMapZ(double worldZ) {
        return worldZ - offsetBlockZ();
    }
}

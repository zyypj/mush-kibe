package br.com.muxe.minigame.bedwars.game;

import java.util.List;

public record Cell(int x, int y, int z) {

    public static Cell of(double x, double y, double z) {
        return new Cell((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public List<Cell> withSides() {
        return List.of(this, new Cell(x + 1, y, z), new Cell(x - 1, y, z), new Cell(x, y, z + 1), new Cell(x, y, z - 1));
    }

    public long key() {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }
}

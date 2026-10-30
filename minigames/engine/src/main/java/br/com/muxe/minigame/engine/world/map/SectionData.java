package br.com.muxe.minigame.engine.world.map;

import java.util.Objects;

public record SectionData(int y, char[] blocks, byte[] blockLight, byte[] skyLight) {

    public static final int BLOCKS = 4096;
    public static final int NIBBLES = 2048;

    public SectionData {
        if (y < 0 || y > 15) {
            throw new IllegalArgumentException("Section y must be between 0 and 15, got " + y);
        }
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(blockLight, "blockLight");
        Objects.requireNonNull(skyLight, "skyLight");
        if (blocks.length != BLOCKS || blockLight.length != NIBBLES || skyLight.length != NIBBLES) {
            throw new IllegalArgumentException("Section " + y + " has arrays of the wrong size");
        }
    }

    public static int index(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    public static char block(int id, int data) {
        return (char) (id << 4 | (data & 15));
    }

    public int id(int x, int y, int z) {
        return blocks[index(x, y, z)] >> 4;
    }

    public int data(int x, int y, int z) {
        return blocks[index(x, y, z)] & 15;
    }

    public boolean isEmpty() {
        for (char block : blocks) {
            if (block != 0) {
                return false;
            }
        }
        return true;
    }
}

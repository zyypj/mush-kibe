package br.com.muxe.minigame.bedwars.game;

import java.util.List;

public final class BridgeEgg {

    public static final String ITEM_ID = "ovo-das-pontes";
    public static final double DROP = 2.0;
    public static final double MAX_DISTANCE = 25.0;
    public static final int MAX_TICKS = 40;
    public static final float SOUND_VOLUME = 1.5f;
    public static final float SOUND_PITCH = 1.49f;

    private BridgeEgg() {
    }

    public static List<Cell> under(double x, double y, double z) {
        return Cell.of(x, y - DROP, z).withSides();
    }

    public static boolean builds(double fromX, double fromY, double fromZ, double x, double y, double z, int ticks) {
        double dx = x - fromX;
        double dy = y - DROP - fromY;
        double dz = z - fromZ;
        return ticks <= MAX_TICKS && dx * dx + dy * dy + dz * dz <= MAX_DISTANCE * MAX_DISTANCE;
    }
}

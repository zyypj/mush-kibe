package br.com.muxe.minigame.bedwars.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class Parachute {

    public static final String ITEM_ID = "para-quedas";
    public static final Duration COOLDOWN = Duration.ofSeconds(5);
    public static final int DROP = 5;
    public static final double LAUNCH = 1.8;
    public static final int PLATFORM_TICKS = 50;
    public static final int WATCH_TICKS = 200;
    public static final float SOUND_VOLUME = 1.5f;
    public static final float LAUNCH_PITCH = 0.49f;
    public static final float BREAK_PITCH = 0.75f;

    private Parachute() {
    }

    public static int platformY(double usedAtY) {
        return (int) Math.floor(usedAtY) - DROP;
    }

    public static boolean reached(double y, int platformY) {
        return y < platformY;
    }

    public static List<Cell> platform(double x, int y, double z) {
        Cell center = Cell.of(x, y, z);
        List<Cell> cells = new ArrayList<>(9);
        cells.add(center);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    cells.add(new Cell(center.x() + dx, y, center.z() + dz));
                }
            }
        }
        return cells;
    }
}

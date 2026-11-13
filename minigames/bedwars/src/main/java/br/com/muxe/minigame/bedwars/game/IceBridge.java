package br.com.muxe.minigame.bedwars.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IceBridge {

    public static final String ITEM_ID = "ponte-de-gelo";
    public static final int LENGTH = 30;
    public static final int TICKS_PER_STEP = 2;
    public static final int LAST_TICK = LENGTH * TICKS_PER_STEP;
    public static final int MELT_TICKS = 50;
    static final List<Double> LANES = List.of(-1.0, -0.5, 0.0, 0.5, 1.0);

    private IceBridge() {
    }

    public static List<Cell> at(int tick, double x, double y, double z, float yaw) {
        if (tick <= 0 || tick > LAST_TICK || tick % TICKS_PER_STEP != 0) {
            return List.of();
        }
        int step = tick / TICKS_PER_STEP;
        double radians = Math.toRadians(yaw);
        double towardX = -Math.sin(radians);
        double towardZ = Math.cos(radians);
        double level = Math.floor(y) - 1;
        Set<Cell> cells = new LinkedHashSet<>();
        for (double lane : LANES) {
            cells.add(Cell.of(x + towardX * step + towardZ * lane, level, z + towardZ * step - towardX * lane));
        }
        return new ArrayList<>(cells);
    }
}

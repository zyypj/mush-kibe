package br.com.muxe.minigame.bedwars.game;

import java.time.Duration;
import java.util.List;

public final class SpiderWeb {

    public static final String ITEM_ID = "spider";
    public static final Duration COOLDOWN = Duration.ofSeconds(7);
    public static final int WEB_TICKS = 140;
    public static final float SHOT_PITCH = 0.75f;
    public static final float WEB_PITCH = 0.79f;
    public static final String SNOWBALL_TAG = "muxe-spider";

    private SpiderWeb() {
    }

    public static List<Cell> webs(double x, double y, double z) {
        return Cell.of(x, y, z).withSides();
    }
}

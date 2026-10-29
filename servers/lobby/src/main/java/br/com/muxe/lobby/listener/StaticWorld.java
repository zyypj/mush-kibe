package br.com.muxe.lobby.listener;

import org.bukkit.World;

public final class StaticWorld {

    public static final String RANDOM_TICK_SPEED = "randomTickSpeed";
    public static final String FIRE_TICK = "doFireTick";

    private StaticWorld() {
    }

    public static void freeze(World world) {
        world.setGameRuleValue(RANDOM_TICK_SPEED, "0");
        world.setGameRuleValue(FIRE_TICK, "false");
    }
}

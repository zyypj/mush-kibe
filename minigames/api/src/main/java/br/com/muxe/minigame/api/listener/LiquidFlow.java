package br.com.muxe.minigame.api.listener;

import org.bukkit.Material;

public final class LiquidFlow {

    private LiquidFlow() {
    }

    public static boolean allowed(Material target, boolean breakOnlyPlaced, boolean placed) {
        return !breakOnlyPlaced || placed || !washesAway(target);
    }

    private static boolean washesAway(Material target) {
        return switch (target) {
            case AIR, WATER, STATIONARY_WATER, LAVA, STATIONARY_LAVA -> false;
            default -> true;
        };
    }
}

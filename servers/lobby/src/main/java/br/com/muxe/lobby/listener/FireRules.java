package br.com.muxe.lobby.listener;

import org.bukkit.Material;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;

public final class FireRules {

    private FireRules() {
    }

    public static boolean spreads(Material source) {
        return source != Material.FIRE;
    }

    public static boolean ignites(IgniteCause cause, boolean placedByBuilder) {
        return switch (cause) {
            case FLINT_AND_STEEL, FIREBALL -> placedByBuilder;
            default -> false;
        };
    }
}

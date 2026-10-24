package br.com.muxe.core.bukkit.buildmode;

import org.bukkit.entity.Player;

public interface BuildModeHook {

    default boolean allows(Player player) {
        return true;
    }

    default void entered(Player player) {
    }

    default void left(Player player) {
    }
}

package br.com.muxe.core.bukkit.buildmode;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.GameMode;

public final class BuildRules {

    private BuildRules() {
    }

    public static boolean allowsGameMode(GameMode target, boolean building) {
        if (target == GameMode.ADVENTURE) {
            return true;
        }
        return building && target == GameMode.CREATIVE;
    }

    public static Optional<Boolean> wants(String argument, boolean building) {
        return switch (argument.toLowerCase(Locale.ROOT)) {
            case "on" -> Optional.of(true);
            case "off" -> Optional.of(false);
            case "" -> Optional.of(!building);
            default -> Optional.empty();
        };
    }
}

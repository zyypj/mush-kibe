package br.com.muxe.minigame.bedwars.cosmetic.profile;

import java.util.function.Predicate;

public enum CosmeticAccess {
    REGULAR,
    ULTRA,
    ALL;

    public static final String ULTRA_PERMISSION = "muxe.cosmetic.ultra";
    public static final String ALL_PERMISSION = "muxe.cosmetic.all";

    public static CosmeticAccess of(Predicate<String> hasPermission) {
        if (hasPermission.test(ALL_PERMISSION)) {
            return ALL;
        }
        return hasPermission.test(ULTRA_PERMISSION) ? ULTRA : REGULAR;
    }
}

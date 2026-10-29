package br.com.muxe.core.bukkit.npc;

public final class NpcRange {

    public static final double SHOW_RADIUS = 48;
    public static final double HIDE_RADIUS = 64;

    private NpcRange() {
    }

    public static Decision decide(boolean shown, double distanceSquared) {
        if (shown) {
            return distanceSquared > HIDE_RADIUS * HIDE_RADIUS ? Decision.HIDE : Decision.KEEP;
        }
        return distanceSquared <= SHOW_RADIUS * SHOW_RADIUS ? Decision.SHOW : Decision.KEEP;
    }

    public enum Decision {
        SHOW,
        HIDE,
        KEEP
    }
}

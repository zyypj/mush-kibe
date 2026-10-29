package br.com.muxe.core.bukkit.shop.checkout;

import java.util.List;

public final class LoadingRing {

    public static final int ROWS = 3;
    public static final List<Integer> SLOTS = List.of(3, 4, 5, 14, 23, 22, 21, 12);

    private LoadingRing() {
    }

    public static int litSlot(long step) {
        return SLOTS.get((int) Math.floorMod(step, (long) SLOTS.size()));
    }

    public static long steps(long durationTicks, long stepTicks) {
        if (stepTicks <= 0) {
            throw new IllegalArgumentException("The loading step needs at least one tick, got " + stepTicks);
        }
        return Math.max(1, durationTicks / stepTicks);
    }
}

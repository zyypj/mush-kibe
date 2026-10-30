package br.com.muxe.core.bukkit.cosmetic.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class Timeline {

    private final List<Step> steps = new ArrayList<>();
    private int length;

    public void at(int tick, Runnable action) {
        repeat(tick, tick, 1, ignored -> action.run());
    }

    public void repeat(int from, int until, int period, IntConsumer action) {
        if (from < 0 || until < from || period < 1) {
            throw new IllegalArgumentException("Invalid repetition " + from + ".." + until + " every " + period);
        }
        steps.add(new Step(from, until, period, action));
        length = Math.max(length, until);
    }

    public void lasts(int ticks) {
        length = Math.max(length, ticks);
    }

    public int length() {
        return length;
    }

    public void run(int tick) {
        for (Step step : List.copyOf(steps)) {
            if (tick >= step.from() && tick <= step.until() && (tick - step.from()) % step.period() == 0) {
                step.action().accept(tick - step.from());
            }
        }
    }

    private record Step(int from, int until, int period, IntConsumer action) {
    }
}

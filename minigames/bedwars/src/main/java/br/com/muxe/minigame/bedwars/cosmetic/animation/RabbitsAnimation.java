package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public final class RabbitsAnimation implements BedAnimation {

    static final double[] HOP_FORWARD = {0.50, 0.44, 0.41, 0.38, 0.34, 0.28, 0.28, 0.25, 0.25, 0.19, 0.19, 0.19, 0.16, 0.16, 0.12};
    static final double[] HOP_UP = {0.59, 0.50, 0.44, 0.34, 0.25, 0.16, 0.09, 0, -0.09, -0.16, -0.22, -0.28, -0.38, -0.47, -0.50};
    static final double DRIFT = sum(HOP_UP) / HOP_UP.length;
    static final double[][] CORNERS = {{1, -0.5}, {1, 0.5}, {-1, -0.5}, {-1, 0.5}};
    static final double BED_LIFT = 0.25;
    static final int START = 5;
    static final int HOPS = 3;
    static final int HOP = HOP_FORWARD.length;
    static final int LAUNCH = START + HOPS * HOP;
    static final int FLIGHT = 30;
    static final int END = LAUNCH + FLIGHT;

    @Override
    public String id() {
        return "coelhos";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec travel = bed.forward().multiply(-1);
        float heading = Vec.ZERO.yawTowards(travel);
        Kit.BedDisplay display = Kit.bed(stage, bed);
        display.move(bed, new Vec(0, BED_LIFT, 0));
        List<Actor> rabbits = new ArrayList<>();
        for (double[] corner : CORNERS) {
            float splay = (float) (corner[1] < 0 ? -10 : 10) * (corner[0] < 0 ? -1 : 1);
            rabbits.add(stage.mob("Rabbit", corner(bed, corner, Vec.ZERO), heading + splay));
        }
        for (int hop = 0; hop <= HOPS; hop++) {
            int landing = START + hop * HOP;
            timeline.at(landing, () -> {
                Vec offset = offset(travel, landing);
                for (double[] corner : CORNERS) {
                    stage.sound("step.grass", corner(bed, corner, offset), 1, 0.79f);
                }
            });
            for (int index = 0; index < rabbits.size(); index++) {
                Actor rabbit = rabbits.get(index);
                timeline.at(landing + index, () -> rabbit.status(1));
            }
        }
        timeline.repeat(START + 1, END, 1, offset -> {
            int tick = START + 1 + offset;
            Vec moved = offset(travel, tick);
            display.move(bed, moved.add(0, BED_LIFT, 0));
            for (int index = 0; index < rabbits.size(); index++) {
                double[] corner = CORNERS[index];
                float splay = (float) (corner[1] < 0 ? -10 : 10) * (corner[0] < 0 ? -1 : 1);
                rabbits.get(index).teleport(corner(bed, corner, moved), heading + splay, 0);
            }
        });
        timeline.at(END, () -> {
            display.remove();
            rabbits.forEach(Actor::remove);
        });
    }

    static Vec offset(Vec travel, int tick) {
        double along = 0;
        double up = 0;
        for (int step = START + 1; step <= Math.min(tick, LAUNCH); step++) {
            int phase = (step - START - 1) % HOP;
            along += HOP_FORWARD[phase];
            up += HOP_UP[phase] - DRIFT;
        }
        for (int step = LAUNCH + 1; step <= tick; step++) {
            int flight = step - LAUNCH - 1;
            along += 3.62 * Math.pow(0.915, flight);
            up += 3.81 - 0.13 * flight;
        }
        return travel.multiply(along).add(0, up, 0);
    }

    private static double sum(double[] values) {
        double total = 0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private static Vec corner(BedFrame bed, double[] corner, Vec offset) {
        return bed.middle().add(bed.forward().multiply(corner[0])).add(bed.right().multiply(corner[1])).add(offset);
    }
}

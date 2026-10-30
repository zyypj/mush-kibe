package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Burst;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.List;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

final class Kit {

    static final int BALL = 0;
    static final int LARGE_BALL = 1;
    static final int STAR = 2;
    static final int CREEPER = 3;
    static final int BURST = 4;
    static final int WHITE = 0xFFFFFF;

    private Kit() {
    }

    static void flash(Stage stage, Timeline timeline, int tick, Vec position, Burst... bursts) {
        timeline.at(tick, () -> {
            Actor rocket = stage.firework(position, List.of(bursts));
            rocket.velocity(new Vec(0, 0.05, 0));
            timeline.at(tick + 1, () -> rocket.status(17));
            timeline.at(tick + 2, rocket::remove);
        });
    }

    static Burst burst(int type, boolean flicker, boolean trail, int... colors) {
        return new Burst(type, List.of(box(colors)), List.of(), flicker, trail);
    }

    static Burst fading(int type, boolean flicker, boolean trail, int color, int fade) {
        return new Burst(type, List.of(color), List.of(fade), flicker, trail);
    }

    static BedDisplay bed(Stage stage, BedFrame bed) {
        int facing = facing(bed);
        Actor footStand = stage.armorStand(bed.foot(), 0, StandStyle.HIDDEN);
        Actor foot = stage.fallingBlock(bed.foot(), Material.BED_BLOCK, facing);
        foot.ride(footStand);
        Actor headStand = stage.armorStand(bed.head(), 0, StandStyle.HIDDEN);
        Actor head = stage.fallingBlock(bed.head(), Material.BED_BLOCK, facing | 8);
        head.ride(headStand);
        return new BedDisplay(footStand, foot, headStand, head);
    }

    static int facing(BedFrame bed) {
        Vec forward = bed.forward();
        if (Math.abs(forward.z()) >= Math.abs(forward.x())) {
            return forward.z() > 0 ? 0 : 2;
        }
        return forward.x() < 0 ? 1 : 3;
    }

    static void dust(Stage stage, Vec position, double red, double green, double blue) {
        stage.particle("REDSTONE", position, 0, new Vec(Math.max(red, 0.02), green, blue), 1);
    }

    static double between(RandomGenerator random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static Integer[] box(int[] values) {
        Integer[] boxed = new Integer[values.length];
        for (int index = 0; index < values.length; index++) {
            boxed[index] = values[index];
        }
        return boxed;
    }

    record BedDisplay(Actor footStand, Actor foot, Actor headStand, Actor head) {

        void remove() {
            foot.remove();
            head.remove();
            footStand.remove();
            headStand.remove();
        }

        void move(BedFrame bed, Vec offset) {
            footStand.teleport(bed.foot().add(offset), 0, 0);
            headStand.teleport(bed.head().add(offset), 0, 0);
        }
    }
}

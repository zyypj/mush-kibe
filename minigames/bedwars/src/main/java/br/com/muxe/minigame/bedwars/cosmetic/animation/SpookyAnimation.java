package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Burst;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Pose;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Slot;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class SpookyAnimation implements BedAnimation {

    static final String GHOST = "cea1b8dd35f356cb66c682eea791df88447281c6d006b82cf853d6945ff6add7";
    static final double[][] PASTEL = {{1, 1, 1}, {0.71, 0.95, 1}, {1, 0.84, 0.96}, {0.78, 0.66, 1}, {0.91, 0.87, 1}};
    static final double[][] POTION = {{0.18, 0.55, 0.18}, {0.69, 0.42, 1}, {0.49, 1, 0.23}, {0.54, 0.17, 0.89}};
    static final double[] EMERGE_RISE = {1.0, 0.5, 0.44, 0.34, 0.25, 0.2};
    static final int GHOSTS = 3;
    static final double START_Y = -0.78;
    static final double HEAD = 0.85;
    static final double RADIUS = 1.5;
    static final double TURN = 14.4;
    static final int EMERGE = EMERGE_RISE.length;
    static final int DIVE = 36;
    static final int SPLASH = 42;
    static final int CAULDRON = 4;
    static final int VANISH = 54;
    static final int[] CHIMES = {15, 21, 27, 33};
    static final float[] CHIME_PITCH = {1.17f, 1.0f, 0.79f, 0.67f};
    static final int[] BUBBLES = {44, 46, 48, 52};
    static final float[] BUBBLE_PITCH = {1.60f, 1.48f, 1.24f, 1.05f};

    @Override
    public String id() {
        return "apavorante";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        Vec foot = bed.foot();
        Kit.BedDisplay display = Kit.bed(stage, bed);
        Burst purple = new Burst(Kit.BALL, List.of(0x8A2BE2, 0x7CFF3A), List.of(0x2E8B2E), false, true);
        Kit.flash(stage, timeline, 0, middle.add(0, 1, 0), purple);
        timeline.at(0, () -> {
            stage.sound("fireworks.largeBlast", middle, 1.5f, 0.79f);
            stage.sound("mob.chicken.plop", middle, 1, 1.59f);
            stage.sound("random.fizz", middle, 0.8f, 0.79f);
            for (int spark = 0; spark < 3; spark++) {
                stage.particle("FIREWORKS_SPARK", middle.add(0, 0.83, 0), 5, Vec.ZERO, 0.08);
            }
        });
        List<Actor> ghosts = new ArrayList<>();
        for (int index = 0; index < GHOSTS; index++) {
            Actor ghost = stage.armorStand(middle.add(0, START_Y, 0), 0, StandStyle.HIDDEN_SMALL);
            ghost.equipSkull(Slot.HELMET, GHOST);
            ghosts.add(ghost);
        }
        timeline.repeat(1, SPLASH - 1, 1, offset -> {
            int tick = offset + 1;
            for (int index = 0; index < GHOSTS; index++) {
                Vec position = ghost(middle, index, tick);
                double angle = angle(index, tick);
                Actor ghost = ghosts.get(index);
                ghost.teleport(position, (float) angle, 0);
                ghost.pose(Pose.STANDING.withHead(new Vec(0, 0, 15 * Math.sin(2 * Math.PI * tick / 16 + index * 2 * Math.PI / GHOSTS))));
                trail(stage, middle, index, tick, random);
            }
        });
        timeline.repeat(1, EMERGE, 1, tick -> {
            for (double y : new double[] {1.54, 2.24, 2.95}) {
                pastel(stage, middle.add(0, y, 0), random);
            }
        });
        timeline.repeat(0, 33, 3, tick -> stage.particle("SPELL_WITCH", foot.add(0, 2.5, 0), 4, new Vec(0.6, 0.2, 0.6), 0));
        Vec pot = foot.add(0, 0.6, 0);
        List<Actor> cauldron = new ArrayList<>();
        timeline.at(CAULDRON, () -> {
            stage.sound("random.fizz", foot.add(0, 0.5, 0), 1, 1.19f);
            stage.sound("liquid.lavapop", foot.add(0, 0.5, 0), 1, 0.79f);
            stage.sound("random.anvil_land", foot.add(0, 0.5, 0), 0.4f, 1.59f);
            stage.particle("SMOKE_LARGE", pot, 10, new Vec(0.4, 0.2, 0.4), 0.02);
            Actor stand = stage.armorStand(foot.add(0, -0.5, 0), 0, StandStyle.HIDDEN);
            Actor block = stage.fallingBlock(foot.add(0, -0.5, 0), Material.CAULDRON, 3);
            block.ride(stand);
            cauldron.add(block);
            cauldron.add(stand);
        });
        for (int chime = 0; chime < CHIMES.length; chime++) {
            float pitch = CHIME_PITCH[chime];
            timeline.at(CHIMES[chime], () -> {
                stage.sound("note.pling", foot.add(0, 2.5, 0), 0.8f, pitch);
                stage.sound("random.orb", foot.add(0, 2.5, 0), 0.3f, (float) Kit.between(random, 1.73, 1.95));
            });
        }
        timeline.at(20, () -> {
            stage.sound("portal.trigger", middle, 0.6f, 1.79f);
            stage.sound("mob.ghast.moan", middle, 0.6f, 1.59f);
        });
        timeline.at(DIVE, () -> stage.sound("fireworks.launch", middle, 1, 0.49f));
        timeline.at(SPLASH, () -> {
            display.remove();
            for (int index = 0; index < GHOSTS; index++) {
                Vec head = ghost(middle, index, DIVE + 3).add(0, HEAD, 0);
                ghosts.get(index).remove();
                stage.particle("EXPLOSION_NORMAL", head, 6, Vec.ZERO, 0.05);
                stage.sound("mob.chicken.plop", head, 0.8f, 1.52f + 0.13f * index);
            }
            stage.sound("random.splash", foot.add(0, 0.5, 0), 1, 0.79f);
            stage.sound("fireworks.blast", foot.add(0, 0.5, 0), 1.5f, 0.70f);
            stage.particle("WATER_SPLASH", pot, 40, new Vec(0.3, 0.1, 0.3), 0);
            for (int dot = 0; dot < 30; dot++) {
                potion(stage, foot.add(Kit.between(random, -0.5, 0.5), Kit.between(random, 0.6, 1.2), Kit.between(random, -0.5, 0.5)), random);
            }
        });
        Kit.flash(stage, timeline, SPLASH, foot.add(0, 1.09, 0), new Burst(Kit.BURST, List.of(0x7CFF3A, 0x8A2BE2), List.of(0x2E8B2E), false, false));
        timeline.repeat(SPLASH + 2, SPLASH + 10, 2, tick -> {
            int dots = 1 + random.nextInt(5);
            for (int dot = 0; dot < dots; dot++) {
                potion(stage, pot.add(Kit.between(random, -0.3, 0.3), Kit.between(random, 0, 0.4), Kit.between(random, -0.3, 0.3)), random);
            }
            stage.particle("SPELL_WITCH", foot.add(0, 0.9, 0), 2, new Vec(0.2, 0.2, 0.2), 0.05);
        });
        for (int bubble = 0; bubble < BUBBLES.length; bubble++) {
            float pitch = BUBBLE_PITCH[bubble];
            timeline.at(BUBBLES[bubble], () -> stage.sound("liquid.lavapop", pot, 0.8f, pitch));
        }
        timeline.at(VANISH, () -> {
            cauldron.forEach(Actor::remove);
            stage.sound("random.fizz", pot, 1, 0.59f);
            stage.particle("SMOKE_LARGE", pot, 15, new Vec(0.4, 0.3, 0.4), 0.03);
            stage.particle("SPELL_WITCH", pot, 15, new Vec(0.3, 0.3, 0.3), 0.2);
        });
    }

    static double angle(int ghost, int tick) {
        return 90 + ghost * 120 + TURN * tick;
    }

    static double radius(int tick) {
        if (tick <= EMERGE) {
            return Math.min(RADIUS, 0.25 * tick);
        }
        if (tick <= DIVE) {
            return RADIUS;
        }
        return Math.max(0, RADIUS - 0.25 * (tick - DIVE));
    }

    static double height(int ghost, int tick) {
        if (tick <= EMERGE) {
            double y = START_Y;
            for (int step = 0; step < tick; step++) {
                y += EMERGE_RISE[step];
            }
            return y;
        }
        int orbit = Math.min(tick, DIVE) - EMERGE;
        double bob = 0.3 * Math.sin(2 * Math.PI * orbit / 19 + ghost * 2 * Math.PI / GHOSTS) * Math.min(1, orbit / 6.0);
        double hover = 2.05 + bob;
        if (tick <= DIVE) {
            return hover;
        }
        return hover + (-0.3 - hover) * Math.min(1, (tick - DIVE) / 6.0);
    }

    static Vec ghost(Vec middle, int ghost, int tick) {
        return middle.add(Kit.horizontal(angle(ghost, tick), radius(tick))).add(0, height(ghost, tick), 0);
    }

    private static void trail(Stage stage, Vec middle, int index, int tick, RandomGenerator random) {
        if (tick < 10) {
            return;
        }
        Vec behind = ghost(middle, index, Math.max(1, tick - 3)).add(0, HEAD, 0);
        if (tick < 20 || tick > DIVE) {
            pastel(stage, behind, random);
            pastel(stage, behind.add(0, -0.2, 0), random);
            return;
        }
        Vec axis = middle.add(0, 2.9, 0);
        for (int point = 0; point < 4; point++) {
            Vec at = tick % 2 == 0 ? behind.add(0, -0.25 * (point + 1), 0) : behind.add(axis.subtract(behind).multiply(0.2 * (point + 1)));
            pastel(stage, at, random);
        }
    }

    private static void pastel(Stage stage, Vec at, RandomGenerator random) {
        double[] color = PASTEL[random.nextInt(PASTEL.length)];
        Kit.dust(stage, at, color[0], color[1], color[2]);
    }

    private static void potion(Stage stage, Vec at, RandomGenerator random) {
        double[] color = POTION[random.nextInt(POTION.length)];
        Kit.dust(stage, at, color[0], color[1], color[2]);
    }
}

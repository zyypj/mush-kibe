package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Pose;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Slot;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class DesertAnimation implements BedAnimation {

    static final String CACTUS = "e156ffc119ecce75f09023f55e74debfcc798a3a0fd2f5388c121cf5497fceba";
    static final double[][] CONFETTI = {
        {0.49, 0.99, 0.02}, {1.00, 0.71, 0.88}, {1.00, 0.84, 0.02}, {1.00, 0.95, 0.63}, {1.00, 1.00, 1.00}
    };
    static final int RISE_FROM = 5;
    static final int RISE_TICKS = 44;
    static final int RISE_UNTIL = RISE_FROM + RISE_TICKS;
    static final double RISE = 8.0;
    static final double SEGMENT = 0.625;
    static final double BASE = -2.0;
    static final double HEAD = 1.75;
    static final int TRUNK = 13;
    static final int[][] LEFT_ARM = {{4, 25}, {3, 26}, {2, 28}};
    static final int[][] RIGHT_ARM = {{8, 34}, {7, 35}, {6, 37}, {5, 39}};
    static final int EXPLOSION = 59;
    static final int RAIN_FROM = 62;
    static final int RAIN_UNTIL = 111;
    static final int POPS_FROM = 66;
    static final int ITEM_LIFETIME = 32;

    @Override
    public String id() {
        return "deserto";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        Kit.BedDisplay display = Kit.bed(stage, bed);
        ground(bed, stage, timeline, random);
        timeline.at(4, () -> {
            stage.particle("BLOCK_DUST", middle.add(0, 0.1, 0), 20, new Vec(0.6, 0.1, 0.6), 0.1, Kit.blockId(Material.SAND, 0));
            stage.sound("dig.sand", middle, 1.2f, 0.70f);
            stage.sound("mob.chicken.plop", middle, 1, 0.79f);
        });
        List<Segment> segments = new ArrayList<>();
        timeline.repeat(RISE_FROM, RISE_UNTIL, 1, offset -> {
            int tick = RISE_FROM + offset;
            double rise = rise(tick);
            display.move(bed, new Vec(0, rise, 0));
            long trunk = segments.stream().filter(segment -> segment.side == 0).count();
            if (trunk < TRUNK && rise >= SEGMENT * trunk - 1.0E-9) {
                int k = (int) trunk;
                segments.add(spawn(bed, stage, random, k, 0, rise));
                stage.sound("note.pling", middle, 0.5f, 0.59f + 0.1f * k);
                stage.sound("dig.cloth", middle, 0.8f, (float) Kit.between(random, 0.8, 1.1));
                stage.particle("BLOCK_DUST", middle, 6, new Vec(0.3, 0.05, 0.3), 0.08, Kit.blockId(Material.SAND, 0));
            }
            for (Segment segment : segments) {
                segment.stand.teleport(position(bed, segment.k, segment.side, rise), segment.yaw, 0);
            }
            if (offset % 2 == 0) {
                stage.particle("BLOCK_DUST", middle, 4, new Vec(0.4, 0.05, 0.4), 0.05, Kit.blockId(Material.SAND, 0));
                stage.particle("BLOCK_DUST", middle.add(0, rise, 0), 2, new Vec(0.4, 0, 0.4), 0, Kit.blockId(Material.SAND, 0));
            }
            if (offset % 3 == 0) {
                stage.particle("VILLAGER_HAPPY", middle, 1, new Vec(0.35, 0.2, 0.35), 0);
            }
            for (int dot = 0; dot < 2; dot++) {
                Vec at = middle.add(bed.right().multiply(Kit.between(random, -0.95, 0.95))).add(0, Kit.between(random, 0, rise), 0);
                confetti(stage, at, random);
                if (random.nextDouble() < 0.35) {
                    stage.particle("FIREWORKS_SPARK", at, 0, Vec.ZERO, 0);
                }
            }
        });
        arm(bed, stage, timeline, random, segments, LEFT_ARM, -1);
        arm(bed, stage, timeline, random, segments, RIGHT_ARM, 1);
        Vec top = middle.add(0, RISE + 0.5, 0);
        timeline.at(RISE_UNTIL, () -> {
            for (int side : new int[] {-1, 1}) {
                Vec tip = tip(bed, side);
                stage.sound("random.orb", tip, 1.2f, 1.59f);
                stage.sound("note.pling", tip, 1.2f, 2.0f);
            }
            stage.sound("creeper.primed", top, 2, 1.19f);
            stage.sound("dig.cloth", top, 2, 0.59f);
        });
        flower(bed, stage, timeline, segments, -1);
        flower(bed, stage, timeline, segments, 1);
        timeline.repeat(RISE_UNTIL + 1, EXPLOSION - 1, 2, tick -> display.move(bed,
            new Vec(Kit.between(random, -0.06, 0.06), RISE, Kit.between(random, -0.06, 0.06))));
        timeline.at(EXPLOSION, () -> {
            display.remove();
            stage.sound("random.explode", top, 3, 1.19f);
            stage.sound("fireworks.largeBlast", top, 3, 1.0f);
            stage.sound("fireworks.twinkle", top, 3, 1.19f);
            stage.particle("EXPLOSION_LARGE", top, 2, new Vec(0.4, 0.3, 0.4), 0);
            stage.particle("CRIT", top, 40, new Vec(0.3, 0.3, 0.3), 0.6);
            stage.particle("BLOCK_DUST", top, 30, new Vec(0.5, 0.3, 0.5), 0.2, Kit.blockId(Material.WOOL, 14));
            stage.particle("VILLAGER_HAPPY", top, 25, new Vec(1.2, 0.8, 1.2), 0);
            for (int dot = 0; dot < 40; dot++) {
                Vec at = top.add(Kit.between(random, -1.5, 1.5), Kit.between(random, -1.5, 1.5), Kit.between(random, -1.5, 1.5));
                confetti(stage, at, random);
                if (dot < 11) {
                    stage.particle("FIREWORKS_SPARK", at, 0, Vec.ZERO, 0);
                }
            }
        });
        Kit.flash(stage, timeline, EXPLOSION, top, Kit.fading(Kit.LARGE_BALL, true, false, 0x006400, 0x32CD32));
        Kit.flash(stage, timeline, EXPLOSION, top, Kit.burst(Kit.BURST, false, true, 0x006400));
        rain(middle, stage, timeline, random);
        pops(bed, stage, timeline, random, segments);
    }

    static double rise(int tick) {
        if (tick < RISE_FROM) {
            return 0;
        }
        double progress = Math.min(1, (tick - RISE_FROM) / (double) RISE_TICKS);
        return RISE * (1 - Math.cos(Math.PI * progress)) / 2;
    }

    static Vec position(BedFrame bed, int k, int side, double rise) {
        return bed.middle().add(bed.right().multiply(side * SEGMENT)).add(0, BASE + rise - SEGMENT * k, 0);
    }

    private static Vec tip(BedFrame bed, int side) {
        int k = side < 0 ? LEFT_ARM[LEFT_ARM.length - 1][0] : RIGHT_ARM[RIGHT_ARM.length - 1][0];
        return position(bed, k, side, RISE).add(0, HEAD + 0.4, 0);
    }

    private static Segment spawn(BedFrame bed, Stage stage, RandomGenerator random, int k, int side, double rise) {
        Actor stand = stage.armorStand(position(bed, k, side, rise), bed.yaw(), StandStyle.HIDDEN);
        stand.equipSkull(Slot.HELMET, CACTUS);
        stand.pose(Pose.STANDING.withHead(new Vec(0, 90 * random.nextInt(3), 0)));
        return new Segment(k, side, stand, bed.yaw());
    }

    private static void arm(BedFrame bed, Stage stage, Timeline timeline, RandomGenerator random, List<Segment> segments, int[][] heads, int side) {
        for (int index = 0; index < heads.length; index++) {
            int k = heads[index][0];
            int tick = heads[index][1];
            float pitch = 1.40f + 0.2f * index;
            timeline.at(tick, () -> {
                segments.add(spawn(bed, stage, random, k, side, rise(tick)));
                Vec head = position(bed, k, side, rise(tick)).add(0, HEAD, 0);
                stage.particle("VILLAGER_HAPPY", head, 5, new Vec(0.25, 0.25, 0.25), 0);
                for (int dot = 0; dot < 3; dot++) {
                    confetti(stage, head.add(Kit.between(random, -0.3, 0.3), Kit.between(random, -0.3, 0.3), Kit.between(random, -0.3, 0.3)), random);
                }
                stage.sound("dig.cloth", head, 0.7f, 1.19f);
                stage.sound("mob.chicken.plop", head, 0.6f, pitch);
            });
        }
    }

    private static void flower(BedFrame bed, Stage stage, Timeline timeline, List<Segment> segments, int side) {
        int k = side < 0 ? LEFT_ARM[LEFT_ARM.length - 1][0] : RIGHT_ARM[RIGHT_ARM.length - 1][0];
        Vec center = tip(bed, side);
        timeline.repeat(RISE_UNTIL + 1, POPS_FROM + 2 * TRUNK + 14, 1, tick -> {
            Segment top = segments.stream().filter(segment -> segment.side == side && segment.k == k).findFirst().orElse(null);
            if (top == null || top.stand.removed()) {
                return;
            }
            double growth = Math.min(1, tick / 6.0);
            double turn = 3.5 * tick;
            Kit.dust(stage, center, 1, 0.84, 0.02);
            for (int petal = 0; petal < 5; petal++) {
                double angle = turn + petal * 72;
                Kit.dust(stage, center.add(Kit.horizontal(angle, 0.035 + 0.12 * growth)).add(0, 0.02 + 0.05 * growth, 0), 1, 0.75, 0.88);
                Kit.dust(stage, center.add(Kit.horizontal(angle, 0.08 + 0.27 * growth)).add(0, 0.03 + 0.09 * growth, 0), 1, 1, 1);
            }
        });
    }

    private static void ground(BedFrame bed, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        int floor = (int) Math.floor(bed.foot().y()) - 1;
        List<List<int[]>> rings = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int x = (int) Math.floor(middle.x()) + dx;
                int z = (int) Math.floor(middle.z()) + dz;
                double distance = Math.hypot(x + 0.5 - middle.x(), z + 0.5 - middle.z());
                int ring = distance < 2 ? 0 : distance < 3 ? 1 : distance <= 3.6 ? 2 : -1;
                if (ring >= 0) {
                    rings.get(ring).add(new int[] {x, z});
                }
            }
        }
        timeline.at(0, () -> {
            stage.sound("dig.sand", middle, 1.2f, 0.59f);
            stage.sound("fireworks.twinkle", middle, 0.6f, 0.79f);
            stage.sound("dig.sand", middle, 0.9f, 0.94f);
            for (int dot = 0; dot < 17; dot++) {
                confetti(stage, middle.add(Kit.between(random, -1.2, 1.2), Kit.between(random, -0.6, 1.5), Kit.between(random, -1.2, 1.2)), random);
            }
        });
        for (int ring = 0; ring < rings.size(); ring++) {
            List<int[]> cells = rings.get(ring);
            timeline.at(ring, () -> {
                stage.sound("dig.sand", middle, 0.9f, (float) Kit.between(random, 0.81, 0.94));
                for (int[] cell : cells) {
                    if (!stage.solid(cell[0], floor, cell[1])) {
                        continue;
                    }
                    double roll = random.nextDouble();
                    Material material = roll < 0.34 ? Material.SAND : Material.SANDSTONE;
                    int data = roll < 0.34 ? 0 : roll < 0.59 ? 0 : roll < 0.81 ? 1 : 2;
                    stage.block(cell[0], floor, cell[1], material, data);
                    Vec face = new Vec(cell[0] + 0.5, floor + 1.0, cell[1] + 0.5);
                    stage.particle("BLOCK_DUST", face, 3, new Vec(0.3, 0.1, 0.3), 0.05, Kit.blockId(material, data));
                    Vec at = face.add(Kit.between(random, -0.5, 0.5), Kit.between(random, 0, 0.6), Kit.between(random, -0.5, 0.5));
                    confetti(stage, at, random);
                    if (random.nextBoolean()) {
                        stage.particle("FIREWORKS_SPARK", at, 0, Vec.ZERO, 0);
                    }
                }
            });
        }
    }

    private static void rain(Vec middle, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec cloud = middle.add(0, RISE + 2.2, 0);
        timeline.at(RAIN_FROM, () -> {
            stage.sound("ambient.weather.thunder", cloud, 0.6f, 1.59f);
            stage.particle("CLOUD", cloud, 30, new Vec(1.4, 0.3, 1.4), 0);
        });
        timeline.repeat(RAIN_FROM + 1, RAIN_UNTIL, 1, tick -> {
            stage.particle("CLOUD", cloud, 3, new Vec(1.4, 0.25, 1.4), 0);
            for (int drop = 0; drop < 4; drop++) {
                stage.particle("DRIP_WATER", cloud.add(Kit.between(random, -1.4, 1.4), -0.3, Kit.between(random, -1.4, 1.4)), 0, Vec.ZERO, 0);
            }
            stage.particle("WATER_SPLASH", middle.add(0, 0.1, 0), 4, new Vec(1.4, 0, 1.4), 0);
        });
        for (int sound : new int[] {74, 89, 104}) {
            timeline.at(sound, () -> stage.sound("ambient.weather.rain", middle, 0.5f, 1.19f));
        }
    }

    private static void pops(BedFrame bed, Stage stage, Timeline timeline, RandomGenerator random, List<Segment> segments) {
        int total = TRUNK + LEFT_ARM.length + RIGHT_ARM.length;
        for (int index = 0; index < total; index++) {
            int order = index;
            int tick = index == 0 ? POPS_FROM : POPS_FROM - 1 + 2 * index;
            float pitch = (float) (1.89 - (1.89 - 0.79) * index / (total - 1));
            timeline.at(tick, () -> {
                List<Segment> sorted = segments.stream()
                    .sorted(Comparator.comparingInt((Segment segment) -> segment.k).thenComparingInt(segment -> Math.abs(segment.side)))
                    .toList();
                if (order >= sorted.size()) {
                    return;
                }
                Segment segment = sorted.get(order);
                Vec head = position(bed, segment.k, segment.side, RISE).add(0, HEAD, 0);
                segment.stand.remove();
                stage.sound("mob.chicken.plop", head, 1.5f, pitch);
                stage.sound("dig.cloth", head, 0.6f, pitch);
                stage.particle("CLOUD", head, 15, new Vec(0.01, 0.01, 0.01), 0.1);
                for (int dot = 0; dot < 4; dot++) {
                    Vec at = head.add(Kit.between(random, -0.3, 0.3), Kit.between(random, -0.3, 0.3), Kit.between(random, -0.3, 0.3));
                    confetti(stage, at, random);
                    if (random.nextBoolean()) {
                        stage.particle("FIREWORKS_SPARK", at, 0, Vec.ZERO, 0);
                    }
                }
                Actor item = stage.item(head, Material.CACTUS, 0);
                item.velocity(new Vec(Kit.between(random, -0.11, 0.11), Kit.between(random, 0.15, 0.30), Kit.between(random, -0.11, 0.11)));
                timeline.at(tick + ITEM_LIFETIME, item::remove);
            });
        }
    }

    private static void confetti(Stage stage, Vec at, RandomGenerator random) {
        double[] color = CONFETTI[random.nextInt(CONFETTI.length)];
        Kit.dust(stage, at, color[0], color[1], color[2]);
    }

    static final class Segment {

        private final int k;
        private final int side;
        private final Actor stand;
        private final float yaw;

        Segment(int k, int side, Actor stand, float yaw) {
            this.k = k;
            this.side = side;
            this.stand = stand;
            this.yaw = yaw;
        }
    }
}

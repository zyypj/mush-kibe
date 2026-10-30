package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Burst;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.List;
import java.util.Map;
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
            stage.sound("fireworks.launch", position, 3, 1);
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

    static void image(Stage stage, BedFrame bed, List<String> rows, Map<Character, double[]> palette, Vec center, double width, double height) {
        double columns = (rows.getFirst().length() - 1) / 2.0;
        double lines = (rows.size() - 1) / 2.0;
        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row);
            for (int column = 0; column < line.length(); column++) {
                double[] color = palette.get(line.charAt(column));
                if (color != null) {
                    dust(stage, pixel(bed, center, column - columns, lines - row, width, height), color[0], color[1], color[2]);
                }
            }
        }
    }

    static Vec pixel(BedFrame bed, Vec center, double column, double row, double width, double height) {
        return center.add(bed.right().multiply(column * width)).add(0, row * height, 0);
    }

    @SuppressWarnings("deprecation")
    static int blockId(Material material, int data) {
        return material.getId() | data << 12;
    }

    static double[] hsv(double hue, double saturation, double value) {
        double h = ((hue % 360) + 360) % 360 / 60;
        int sector = (int) Math.floor(h);
        double fraction = h - sector;
        double p = value * (1 - saturation);
        double q = value * (1 - saturation * fraction);
        double t = value * (1 - saturation * (1 - fraction));
        return switch (sector) {
            case 0 -> new double[] {value, t, p};
            case 1 -> new double[] {q, value, p};
            case 2 -> new double[] {p, value, t};
            case 3 -> new double[] {p, q, value};
            case 4 -> new double[] {t, p, value};
            default -> new double[] {value, p, q};
        };
    }

    static int rgb(double[] color) {
        return (int) Math.round(color[0] * 255) << 16 | (int) Math.round(color[1] * 255) << 8 | (int) Math.round(color[2] * 255);
    }

    static double eased(int tick, int ticks, double total) {
        return total * curve(Math.min(tick, ticks)) / curve(ticks);
    }

    static Vec horizontal(double degrees, double length) {
        double radians = Math.toRadians(degrees);
        return new Vec(Math.cos(radians) * length, 0, Math.sin(radians) * length);
    }

    static double between(RandomGenerator random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static double curve(int tick) {
        return 0.06 * tick + 0.0235 * tick * tick;
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

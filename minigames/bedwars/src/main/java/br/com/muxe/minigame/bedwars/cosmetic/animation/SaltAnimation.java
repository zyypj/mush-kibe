package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

public final class SaltAnimation implements BedAnimation {

    static final List<String> SHAKER = List.of(
        "..OOOOOOOOO..",
        ".OSSSSSSSSSO.",
        "OSSSSSSSSSSSO",
        "OSSSSSSSSSSSO",
        "OSSSSSSSSSSSO",
        "OGHGGGGGGGGGO",
        "OGHGGGGGGGGGO",
        "OGHGGGGGGGGGO",
        "OGHGGGGGGGGGO",
        "OGHGGGGGGGGGO",
        ".OGHGGGGGGGO.",
        ".OGHGGGGGGGO.",
        ".OGGGGGGGGGO.",
        ".RRRRRRRRRRR.",
        ".CCXCCXCCXCC.",
        "..CCXCCXCCC..",
        "....CCXCC...."
    );
    static final Map<Character, double[]> PALETTE = Map.of(
        'S', new double[] {1, 1, 1},
        'H', new double[] {1, 1, 1},
        'G', new double[] {0.62, 0.85, 0.93},
        'O', new double[] {0.31, 0.34, 0.37},
        'R', new double[] {0.17, 0.18, 0.20},
        'C', new double[] {0.29, 0.31, 0.33},
        'X', new double[] {0.02, 0.02, 0.02}
    );
    static final double PIXEL = 0.11;
    static final double TOP = 3.38;
    static final int SHAKE_FROM = 6;
    static final int END = 46;

    @Override
    public String id() {
        return "sal-na-ferida";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        Kit.flash(stage, timeline, 0, middle.add(0, 1, 0),
            Kit.fading(Kit.LARGE_BALL, false, true, Kit.WHITE, 0xC0C0C0), Kit.burst(Kit.BURST, true, false, Kit.WHITE));
        timeline.at(0, () -> {
            stage.particle("EXPLOSION_NORMAL", middle.add(0, 1, 0), 40, new Vec(0.5, 0.3, 0.5), 0.2);
            stage.sound("fireworks.twinkle", middle, 1, 1.19f);
            stage.sound("mob.chicken.plop", middle, 0.8f, 0.79f);
        });
        timeline.repeat(0, END, 2, tick -> {
            double shake = shake(tick);
            draw(stage, bed, shake);
            for (int glint = 0; glint < 2; glint++) {
                int column = random.nextInt(13);
                int row = random.nextInt(SHAKER.size());
                stage.particle("FIREWORKS_SPARK", pixel(bed, column, row, shake), 1, Vec.ZERO, 0);
            }
            stage.particle("EXPLOSION_NORMAL", middle.add(0, 0.5, 0), 1, new Vec(1.2, 0.3, 1.2), 0.02);
        });
        timeline.repeat(SHAKE_FROM, END - 1, 1, tick -> stage.particle("SNOWBALL",
            middle.add(bed.right().multiply(shake(tick + SHAKE_FROM))).add(0, 1.57, 0), 3, new Vec(0.2, 0.05, 0.2), 0));
        timeline.repeat(SHAKE_FROM, END - 1, 4, tick -> stage.sound("dig.sand", middle.add(0, 2.5, 0), 0.5f, (float) Kit.between(random, 1.4, 1.8)));
        timeline.repeat(SHAKE_FROM, END - 1, 8, tick -> {
            stage.particle("SMOKE_NORMAL", middle, 4, new Vec(0.5, 0.1, 0.5), 0.02);
            stage.sound("random.fizz", middle, 0.4f, (float) Kit.between(random, 1.27, 1.48));
        });
        timeline.at(END, () -> {
            stage.particle("FIREWORKS_SPARK", middle.add(0, 2.5, 0), 25, Vec.ZERO, 0.15);
            stage.sound("game.neutral.hurt", middle, 1, 1.19f);
            stage.sound("fireworks.twinkle", middle.add(0, 2.5, 0), 0.8f, 1.59f);
        });
    }

    static double shake(int tick) {
        return tick < SHAKE_FROM ? 0 : -0.06 * Math.sin(0.8 * (tick - SHAKE_FROM));
    }

    static Vec pixel(BedFrame bed, int column, int row, double shake) {
        return bed.middle().add(bed.right().multiply((column - 6) * PIXEL + shake)).add(0, TOP - row * PIXEL, 0);
    }

    private static void draw(Stage stage, BedFrame bed, double shake) {
        for (int row = 0; row < SHAKER.size(); row++) {
            String line = SHAKER.get(row);
            for (int column = 0; column < line.length(); column++) {
                double[] color = PALETTE.get(line.charAt(column));
                if (color != null) {
                    Kit.dust(stage, pixel(bed, column, row, shake), color[0], color[1], color[2]);
                }
            }
        }
    }
}

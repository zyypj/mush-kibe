package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class PumpkinHarvestAnimation implements BedAnimation {

    static final List<String> LANTERN = List.of(
        "......Ss.....",
        "...OOORROO...",
        ".OOOROOOROOO.",
        "OOYYROOORYYOO",
        "OYYYROOORYYYO",
        "OOOOROYOROOOO",
        "OOOOROOOROOOO",
        "OYYYYYYYYYYYO",
        "OOYYOYYYOYYOO",
        ".OOOROOOROOO.",
        "...OORRROO..."
    );
    static final Map<Character, double[]> PALETTE = Map.of(
        'O', new double[] {0.95, 0.55, 0.12},
        'Y', new double[] {1.0, 0.88, 0.30},
        'R', new double[] {0.76, 0.34, 0.06},
        'S', new double[] {0.36, 0.55, 0.16},
        's', new double[] {0.24, 0.48, 0.12}
    );
    static final double PIXEL = 0.12;
    static final double HEIGHT = 1.9;
    static final int BURST = 16;
    static final int[] BEATS = {4, 7, 10, 13};
    static final float[] DRUM = {0.59f, 0.79f, 1.0f, 1.19f};
    static final float[] PLING = {0.70f, 1.02f, 1.35f, 1.67f};
    static final double[] SCALE = {0.5, 0.56, 0.63, 0.75, 0.84, 1.0, 1.12, 1.26, 1.5, 1.68, 2.0};
    static final double[] POP = {0.82, 1.08, 1.07};
    static final int ITEMS = 16;
    static final int STREAM_FROM = 17;
    static final int FADE_FROM = 21;
    static final int FADE_UNTIL = 27;
    static final int END = 100;

    @Override
    public String id() {
        return "colheita-de-aboboras";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        Vec center = middle.add(0, HEIGHT, 0);
        int pumpkin = Kit.blockId(Material.PUMPKIN, 0);
        timeline.at(0, () -> {
            stage.particle("BLOCK_DUST", middle.add(0, 0.6, 0), 15, new Vec(0.4, 0.1, 0.4), 0.1, pumpkin);
            stage.sound("random.pop", middle.add(0, 2, 0), 1, 0.59f);
            stage.sound("dig.wood", middle.add(0, 2, 0), 1, 0.79f);
            stage.sound("mob.ghast.moan", middle.add(0, 2, 0), 0.5f, 1.49f);
            stage.sound("dig.grass", middle, 1, 0.70f);
        });
        timeline.repeat(0, BURST - 1, 1, tick -> {
            double scale = tick < POP.length ? POP[tick] : 1;
            double wobble = wobble(tick);
            Kit.image(stage, bed, LANTERN, PALETTE, center, PIXEL * scale * (1 + wobble), PIXEL * scale * (1 - wobble));
        });
        for (int beat = 0; beat < BEATS.length; beat++) {
            int index = beat;
            timeline.at(BEATS[beat], () -> {
                stage.sound("note.bd", middle.add(0, 2, 0), 0.8f, DRUM[index]);
                stage.sound("note.pling", middle.add(0, 2, 0), 0.5f, PLING[index]);
                Vec spark = Kit.pixel(bed, center, random.nextInt(13) - 6, random.nextInt(11) - 5, PIXEL, PIXEL);
                stage.particle("FIREWORKS_SPARK", spark, 1, Vec.ZERO, 0);
            });
        }
        timeline.at(BURST, () -> {
            stage.particle("EXPLOSION_LARGE", middle.add(0, 2, 0), 1, Vec.ZERO, 0);
            stage.particle("BLOCK_DUST", middle.add(0, 2, 0), 40, new Vec(0.6, 0.6, 0.6), 0.1, pumpkin);
            stage.sound("random.explode", middle.add(0, 2, 0), 0.7f, 1.59f);
            stage.sound("fireworks.blast", middle.add(0, 2, 0), 1, 0.70f);
            stage.sound("dig.wood", middle.add(0, 2, 0), 1, 0.59f);
        });
        Kit.flash(stage, timeline, BURST, middle.add(0, 2, 0), Kit.burst(Kit.BALL, false, false, 0xFF7A00, 0x5B8C2A));
        scatter(bed, center, stage, timeline, random);
        List<Actor> items = new ArrayList<>();
        for (int index = 0; index < ITEMS; index++) {
            harvest(middle, stage, timeline, random, index, items);
        }
        timeline.at(END, () -> items.forEach(Actor::remove));
    }

    static double wobble(int tick) {
        double amplitude = 0.02 + 0.06 * tick / (BURST - 1.0);
        return amplitude * Math.sin(2 * Math.PI * tick / 4.5);
    }

    static float pitch(int item) {
        return (float) SCALE[Math.min(SCALE.length - 1, item * 2 / 3)];
    }

    static boolean high(int item) {
        return item % 3 == 2;
    }

    private static void scatter(BedFrame bed, Vec center, Stage stage, Timeline timeline, RandomGenerator random) {
        List<Pixel> pixels = new ArrayList<>();
        for (int row = 0; row < LANTERN.size(); row++) {
            String line = LANTERN.get(row);
            for (int column = 0; column < line.length(); column++) {
                double[] color = PALETTE.get(line.charAt(column));
                if (color != null) {
                    Vec start = Kit.pixel(bed, center, column - 6, 5 - row, PIXEL, PIXEL);
                    Vec velocity = start.subtract(center).multiply(0.5).add(bed.forward().multiply(Kit.between(random, -0.35, 0.35)));
                    pixels.add(new Pixel(start, velocity, color, FADE_FROM + random.nextInt(FADE_UNTIL - FADE_FROM + 1)));
                }
            }
        }
        timeline.repeat(BURST, FADE_UNTIL, 1, tick -> {
            for (Pixel pixel : pixels) {
                if (pixel.dead) {
                    continue;
                }
                if (BURST + tick >= pixel.dies) {
                    pixel.dead = true;
                    stage.particle("SMOKE_NORMAL", pixel.position, 1, Vec.ZERO, 0.01);
                    continue;
                }
                Kit.dust(stage, pixel.position, pixel.color[0], pixel.color[1], pixel.color[2]);
                pixel.position = pixel.position.add(pixel.velocity);
                pixel.velocity = pixel.velocity.multiply(0.88).add(0, -0.015, 0);
            }
        });
    }

    private static void harvest(Vec middle, Stage stage, Timeline timeline, RandomGenerator random, int index, List<Actor> items) {
        int launch = STREAM_FROM + index * 2;
        boolean high = high(index);
        double angle = random.nextDouble() * 360;
        Vec velocity = high
            ? Kit.horizontal(angle, Kit.between(random, 0.04, 0.10)).add(0, 0.8, 0)
            : Kit.horizontal(angle, Kit.between(random, 0.23, 0.27)).add(0, Kit.between(random, 0.21, 0.30), 0);
        Vec start = middle.add(0, 2, 0);
        Body body = Body.item(start, velocity);
        int kind = random.nextInt(16);
        Material material = kind < 7 ? Material.PUMPKIN_PIE : kind < 14 ? Material.JACK_O_LANTERN : Material.PUMPKIN;
        timeline.at(launch, () -> {
            Actor item = stage.item(start, material, 0);
            item.velocity(velocity);
            items.add(item);
            stage.sound("mob.chicken.plop", start, 0.8f, pitch(index));
            stage.sound("note.pling", start, 0.6f, pitch(index));
            if (high) {
                stage.sound("fireworks.launch", start, 0.8f, 1.4f);
            }
        });
        timeline.repeat(launch + 1, END - 1, 1, tick -> {
            if (body.landed()) {
                return;
            }
            boolean landed = body.step(stage);
            Vec at = body.position();
            if (high && !landed) {
                stage.particle("FIREWORKS_SPARK", at, 1, Vec.ZERO, 0);
            }
            if (landed) {
                land(stage, random, at, high);
            }
        });
    }

    private static void land(Stage stage, RandomGenerator random, Vec at, boolean high) {
        int pumpkin = Kit.blockId(Material.PUMPKIN, 0);
        stage.sound("dig.wood", at, 0.7f, (float) Kit.between(random, 0.94, 1.08));
        if (high) {
            stage.sound("fireworks.twinkle", at, 0.6f, (float) Kit.between(random, 1.6, 1.75));
            stage.particle("BLOCK_DUST", at.add(0, 0.1, 0), 12, new Vec(0.2, 0.05, 0.2), 0.1, pumpkin);
            stage.particle("FIREWORKS_SPARK", at.add(0, 0.1, 0), 8, Vec.ZERO, 0.1);
        } else {
            stage.sound("random.pop", at, 0.4f, (float) Kit.between(random, 1.6, 1.9));
            stage.particle("BLOCK_DUST", at.add(0, 0.1, 0), 6, new Vec(0.2, 0.05, 0.2), 0.1, pumpkin);
        }
    }

    static final class Pixel {

        private final double[] color;
        private final int dies;
        private Vec position;
        private Vec velocity;
        private boolean dead;

        Pixel(Vec position, Vec velocity, double[] color, int dies) {
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.dies = dies;
        }
    }
}

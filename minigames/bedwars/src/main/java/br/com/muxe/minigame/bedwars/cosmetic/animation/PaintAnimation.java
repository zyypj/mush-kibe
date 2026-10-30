package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Slot;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class PaintAnimation implements BedAnimation {

    static final String BUCKET = "fef3490dc625bd4e7b5c24ae3d0a1669b30cd1c0064c47d00800e38a5408ec10";
    static final int FALL = 21;
    static final double BUCKET_HEIGHT = 10.56;
    static final double BUCKET_DEPTH = 11.46;
    static final double HEAD = 1.6;
    static final double BLOB_HEAD = 1.38;
    static final int STAR_FROM = 23;
    static final int STAR_FRAMES = 16;
    static final int FINALE = 54;
    static final int END = 78;
    static final int FLIGHT_LIMIT = 50;
    static final List<Paint> PAINTS = List.of(
        new Paint(11, 0x0000FF, 0.02, 0.02, 1),
        new Paint(5, 0x00FF00, 0.02, 1, 0.02),
        new Paint(4, 0xFFFF00, 1, 1, 0.02),
        new Paint(14, 0xFF0000, 1, 0.02, 0.02)
    );
    static final double[] LAUNCH_SPEED = {0.6, 0.8, 1.0, 1.2};
    static final double[] SPIN = {87, 71, 54, 44, 32, 26, 16, 13, 7, 6, 3, 1};
    static final double[] STAR_SCALE = {0.07, 0.095, 0.11, 0.115, 0.12};
    static final List<String> STAR = star(21, 22);

    @Override
    public String id() {
        return "tinta";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        Kit.BedDisplay display = Kit.bed(stage, bed);
        Actor bucket = stage.armorStand(bucketAt(middle, 0), 0, StandStyle.HIDDEN);
        bucket.equipSkull(Slot.HELMET, BUCKET);
        timeline.at(0, () -> stage.sound("random.pop", middle.add(0, 0.5, 0), 1, 0.49f));
        timeline.repeat(1, FALL, 1, tick -> bucket.teleport(bucketAt(middle, tick), -6 * tick, 0));
        timeline.repeat(0, FALL - 1, 2, tick -> {
            Vec emitter = tick < 4 ? bucketAt(middle, 0).add(0, HEAD + 0.5 * (tick + 1), 0) : bucketAt(middle, tick - 4).add(0, HEAD, 0);
            for (Paint paint : PAINTS) {
                Kit.dust(stage, emitter.add(Kit.between(random, -0.3, 0.3), Kit.between(random, 0, 0.9), Kit.between(random, -0.3, 0.3)),
                    paint.red(), paint.green(), paint.blue());
            }
        });
        timeline.at(FALL, () -> {
            display.remove();
            stage.sound("random.anvil_land", middle.add(0, 0.5, 0), 0.4f, 1.59f);
            stage.sound("random.splash", middle, 1, 1.19f);
            stage.sound("mob.slime.small", middle, 1, 0.79f);
            for (Paint paint : PAINTS) {
                stage.particle("BLOCK_DUST", middle.add(0, 0.1, 0), 15, new Vec(0.6, 0.05, 0.6), 0.1, Kit.blockId(Material.WOOL, paint.wool()));
            }
        });
        timeline.at(FALL + 2, bucket::remove);
        timeline.repeat(FALL, FALL + 5, 1, tick -> ring(stage, middle.add(0, 0.1, 0), (tick + 1) / 3.0, random));
        double base = random.nextDouble() * 360;
        List<Actor> sticks = new ArrayList<>();
        for (int index = 0; index < PAINTS.size(); index++) {
            launch(bed, stage, timeline, random, index, base + index * 90, sticks);
        }
        timeline.repeat(STAR_FROM, STAR_FROM + (STAR_FRAMES - 1) * 2, 2, tick -> drawStar(stage, bed, tick / 2));
        timeline.at(FINALE, () -> {
            Vec center = starCenter(middle, STAR_FRAMES - 1);
            int[] colors = new int[4];
            for (int index = 0; index < colors.length; index++) {
                colors[index] = Kit.rgb(starColor(STAR_FRAMES - 1, index / 3.0));
            }
            stage.sound("fireworks.twinkle", center, 1.5f, 1);
            Kit.flash(stage, timeline, FINALE + 1, center, Kit.burst(Kit.STAR, false, false, colors));
        });
        timeline.at(END, () -> sticks.forEach(Actor::remove));
    }

    static Vec bucketAt(Vec middle, int tick) {
        return middle.add(0, BUCKET_HEIGHT - Kit.eased(tick, FALL, BUCKET_DEPTH), 0);
    }

    static Vec starCenter(Vec middle, int frame) {
        if (frame < STAR_SCALE.length) {
            return middle.add(0, 2.0 + 1.5 * frame / (STAR_SCALE.length - 1), 0);
        }
        return middle.add(0, 3.35 + 0.15 * Math.cos(Math.PI * (frame - STAR_SCALE.length + 1) / 10), 0);
    }

    static double[] starColor(int frame, double height) {
        double hue = 291 + 11 * frame + 40 * height;
        return Kit.hsv(hue, 0.8 - 0.47 * height, 0.69 + 0.31 * height);
    }

    static List<String> star(int width, int height) {
        double[][] points = new double[10][];
        for (int index = 0; index < 10; index++) {
            double radius = index % 2 == 0 ? 1 : 0.42;
            double angle = Math.toRadians(-90 + index * 36);
            points[index] = new double[] {Math.cos(angle) * radius, Math.sin(angle) * radius};
        }
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            StringBuilder line = new StringBuilder();
            for (int column = 0; column < width; column++) {
                double x = (column - (width - 1) / 2.0) / ((width - 1) / 2.0);
                double y = (row - (height - 1) / 2.0) / ((height - 1) / 2.0) * 1.05 + 0.08;
                line.append(inside(points, x, y) ? '#' : '.');
            }
            rows.add(line.toString());
        }
        return List.copyOf(rows);
    }

    private static boolean inside(double[][] polygon, double x, double y) {
        boolean inside = false;
        for (int index = 0, previous = polygon.length - 1; index < polygon.length; previous = index++) {
            double[] a = polygon[index];
            double[] b = polygon[previous];
            if ((a[1] > y) != (b[1] > y) && x < (b[0] - a[0]) * (y - a[1]) / (b[1] - a[1]) + a[0]) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static void drawStar(Stage stage, BedFrame bed, int frame) {
        double spacing = STAR_SCALE[Math.min(frame, STAR_SCALE.length - 1)];
        Vec center = starCenter(bed.middle(), frame);
        int rows = STAR.size();
        for (int row = 0; row < rows; row++) {
            double[] color = starColor(frame, 1 - row / (rows - 1.0));
            String line = STAR.get(row);
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == '#') {
                    Vec at = Kit.pixel(bed, center, column - (line.length() - 1) / 2.0, (rows - 1) / 2.0 - row, spacing, spacing);
                    Kit.dust(stage, at, color[0], color[1], color[2]);
                }
            }
        }
    }

    private static void ring(Stage stage, Vec center, double radius, RandomGenerator random) {
        int points = (int) Math.round(2 * Math.PI * radius / 0.2);
        for (int index = 0; index < points; index++) {
            Paint paint = PAINTS.get(random.nextInt(PAINTS.size()));
            Kit.dust(stage, center.add(Kit.horizontal(360.0 * index / points, radius)), paint.red(), paint.green(), paint.blue());
        }
        for (int index = 0; index < 10; index++) {
            Paint paint = PAINTS.get(random.nextInt(PAINTS.size()));
            Kit.dust(stage, center.add(Kit.horizontal(random.nextDouble() * 360, random.nextDouble() * radius)), paint.red(), paint.green(), paint.blue());
        }
    }

    private static void launch(BedFrame bed, Stage stage, Timeline timeline, RandomGenerator random, int index, double angle, List<Actor> sticks) {
        Paint paint = PAINTS.get(index);
        int start = FALL + index * 2;
        Vec middle = bed.middle();
        Body body = Body.living(middle, Kit.horizontal(angle, 0.27).add(0, LAUNCH_SPEED[index], 0));
        Blob blob = new Blob(body);
        timeline.at(start, () -> {
            blob.stand = stage.armorStand(middle.add(0, -BLOB_HEAD, 0), 0, new StandStyle(true, false, false, false, false));
            blob.stand.equip(Slot.HELMET, Material.WOOL, paint.wool());
            stage.sound("random.pop", middle, 1, 0.79f + 0.2f * index);
            stage.sound("mob.chicken.plop", middle, 1, 0.89f + 0.15f * index);
        });
        Vec squirt = middle.add(Kit.horizontal(random.nextDouble() * 360, random.nextDouble() * 0.3));
        timeline.repeat(start + 2, start + 4, 1, tick -> {
            Vec at = squirt.add(0, 2.5 + 1.66 * tick, 0);
            for (int dot = 0; dot < 10; dot++) {
                Kit.dust(stage, at.add(Kit.between(random, -0.1, 0.1), Kit.between(random, -0.1, 0.1), Kit.between(random, -0.1, 0.1)), paint.red(), paint.green(), paint.blue());
            }
        });
        timeline.at(start + 5, () -> line(stage, squirt.add(0, 5.8, 0), blob.body.position(), 0.05, 0, paint, random));
        timeline.repeat(start + 1, start + FLIGHT_LIMIT, 1, tick -> {
            if (blob.splatted || blob.stand == null) {
                return;
            }
            Vec before = blob.body.position();
            boolean landed = blob.body.step(stage);
            Vec after = blob.body.position();
            float yaw = tick < SPIN.length ? blob.yaw += (float) SPIN[tick] : blob.yaw;
            blob.stand.teleport(after.add(0, -BLOB_HEAD, 0), yaw, 0);
            if (tick >= 5) {
                line(stage, before, after, 0.05, 0.12, paint, random);
            }
            if (landed) {
                blob.splatted = true;
                splat(bed, stage, timeline, random, paint, after, start + 1 + tick, sticks);
                timeline.at(start + 5 + tick, blob.stand::remove);
            }
        });
        timeline.at(start + FLIGHT_LIMIT + 1, () -> {
            if (blob.stand != null) {
                blob.stand.remove();
            }
        });
    }

    private static void splat(BedFrame bed, Stage stage, Timeline timeline, RandomGenerator random, Paint paint, Vec landing, int tick, List<Actor> sticks) {
        Vec spark = landing.add(0, Kit.between(random, 0.7, 1.2), 0);
        Kit.flash(stage, timeline, tick + 1, spark, Kit.burst(Kit.STAR, false, false, paint.color()));
        stage.sound("random.pop", landing, 1, (float) Kit.between(random, 0.5, 0.65));
        stage.sound("mob.slime.small", landing, 0.8f, (float) Kit.between(random, 0.9, 1.16));
        stage.sound("mob.chicken.plop", landing, 1, (float) Kit.between(random, 1.2, 1.57));
        stage.sound("dig.cloth", landing, 0.7f, 0.9f);
        stage.sound("dig.cloth", landing, 0.7f, 1.1f);
        Actor stick = stage.item(spark, Material.STICK, 0);
        stick.velocity(new Vec(Kit.between(random, -0.1, 0.1), 0.3, Kit.between(random, -0.1, 0.1)));
        sticks.add(stick);
        int floor = (int) Math.floor(landing.y()) - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = (int) Math.floor(landing.x()) + dx;
                int z = (int) Math.floor(landing.z()) + dz;
                double distance = new Vec(x + 0.5, landing.y(), z + 0.5).distance(landing);
                if (distance <= 2.0 && random.nextDouble() < 0.65 && stage.solid(x, floor, z) && !stage.solid(x, floor + 1, z)) {
                    stage.block(x, floor, z, Material.WOOL, paint.wool());
                    stage.particle("BLOCK_DUST", new Vec(x + 0.5, floor + 1.05, z + 0.5), 3, new Vec(0.3, 0.05, 0.3), 0.05, Kit.blockId(Material.WOOL, paint.wool()));
                }
            }
        }
        int dots = 14 + random.nextInt(5);
        for (int dot = 0; dot < dots; dot++) {
            Kit.dust(stage, landing.add(Kit.horizontal(random.nextDouble() * 360, random.nextDouble() * 1.5)).add(0, Kit.between(random, -0.1, 0.1), 0),
                paint.red(), paint.green(), paint.blue());
        }
    }

    private static void line(Stage stage, Vec from, Vec to, double spacing, double jitter, Paint paint, RandomGenerator random) {
        double length = from.distance(to);
        int dots = Math.max(1, (int) Math.ceil(length / spacing));
        Vec step = to.subtract(from).multiply(1.0 / dots);
        for (int dot = 0; dot <= dots; dot++) {
            Vec at = from.add(step.multiply(dot));
            if (jitter > 0) {
                at = at.add(Kit.between(random, -jitter, jitter), Kit.between(random, -jitter, jitter), Kit.between(random, -jitter, jitter));
            }
            Kit.dust(stage, at, paint.red(), paint.green(), paint.blue());
        }
    }

    record Paint(int wool, int color, double red, double green, double blue) {
    }

    static final class Blob {

        private final Body body;
        private Actor stand;
        private float yaw;
        private boolean splatted;

        Blob(Body body) {
            this.body = body;
        }
    }
}

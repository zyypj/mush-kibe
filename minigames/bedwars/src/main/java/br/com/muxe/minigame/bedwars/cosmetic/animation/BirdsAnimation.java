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

public final class BirdsAnimation implements BedAnimation {

    static final List<Bird> BIRDS = List.of(
        new Bird("161e257da263849c5d7c5105c558dcb7d1adb2804a57744e8956d9ba9d6e737c", 14),
        new Bird("9f2fc64bcbe3a1f6bd7f038d8f0f0aa3c6f957b9f298c25188c3c3bf55fdfa4c", 3),
        new Bird("26e2b6eb3b1e7723d0d019405c7d1d240bcdde6f754fe8b53db8aa2eba1c8983", 4),
        new Bird("7f0759f45361959036bc1621cd9ba4befae30edf4b4e9ff019aec07074766adf", 11)
    );
    static final double TURN = 12;
    static final double START_RADIUS = 3;
    static final double HOVER_RADIUS = 1;
    static final double START_Y = -1.44;
    static final double HOVER_Y = 4.85;
    static final double HEAD = 1.6;
    static final int SPIRAL = 39;
    static final int BURST = 54;
    static final double[] FLY_RADIUS = {0.10, 0.27, 0.45, 0.78, 1.2, 1.7, 2.4, 2.4, 1.7, 1.2};
    static final double[] FLY_RISE = {0.06, 0.34, 0.31, 0.62, 0.75, 1.16, 1.53, 1.47, 1.12, 0.59};
    static final int END = BURST + FLY_RADIUS.length;

    @Override
    public String id() {
        return "passarinhos";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        double base = Math.toDegrees(Math.atan2(bed.forward().z(), bed.forward().x()));
        List<Actor> stands = new ArrayList<>();
        for (int index = 0; index < BIRDS.size(); index++) {
            double angle = base + index * 90;
            Vec position = place(middle, angle, START_RADIUS, START_Y);
            Actor stand = stage.armorStand(position, middle.yawTowards(position), new StandStyle(true, false, false, false, false));
            stand.equipSkull(Slot.HELMET, BIRDS.get(index).texture());
            stands.add(stand);
        }
        timeline.at(0, () -> {
            Vec head = place(middle, base, START_RADIUS, START_Y + HEAD);
            stage.sound("mob.bat.loop", head, 1, 1);
            stage.sound("mob.bat.idle", head, 1, 1);
        });
        timeline.repeat(1, BURST, 1, tick -> {
            for (int index = 0; index < stands.size(); index++) {
                Vec position = place(middle, base + index * 90 - TURN * tick, radius(tick), height(tick, index));
                stands.get(index).teleport(position, middle.yawTowards(position), 0);
            }
        });
        timeline.repeat(20, BURST, 20, tick -> {
            int index = random.nextInt(BIRDS.size());
            Vec head = place(middle, base + index * 90 - TURN * (tick + 20), radius(tick + 20), height(tick + 20, index) + HEAD);
            stage.sound("mob.bat.idle", head, 1, 1);
            if (tick == 20) {
                stage.sound("mob.bat.loop", head, 1, 1);
            }
        });
        timeline.at(BURST, () -> {
            for (int index = 0; index < BIRDS.size(); index++) {
                Vec head = place(middle, base + index * 90 - TURN * BURST, HOVER_RADIUS, height(BURST, index) + HEAD);
                stage.particle("BLOCK_DUST", head, 50, new Vec(0, 0.25, 0), 0.1, Kit.blockId(Material.WOOL, BIRDS.get(index).wool()));
                stage.sound("fireworks.launch", head, 1, 1);
            }
        });
        for (int death : new int[] {BURST + 1, BURST + 5}) {
            timeline.at(death, () -> stage.sound("mob.bat.death", place(middle, base, HOVER_RADIUS, HOVER_Y + HEAD), 1, 2));
        }
        timeline.repeat(BURST + 1, END - 1, 1, tick -> {
            double radius = HOVER_RADIUS;
            double rise = height(BURST, 0);
            for (int step = 0; step <= tick; step++) {
                radius += FLY_RADIUS[step];
                rise += FLY_RISE[step];
            }
            for (int index = 0; index < stands.size(); index++) {
                Vec position = place(middle, base + index * 90 - TURN * (BURST + 1 + tick), radius, rise);
                stands.get(index).teleport(position, middle.yawTowards(position), 0);
            }
        });
        timeline.at(END, () -> stands.forEach(Actor::remove));
    }

    static double radius(int tick) {
        if (tick >= SPIRAL) {
            return HOVER_RADIUS;
        }
        return START_RADIUS - (START_RADIUS - HOVER_RADIUS) * tick / SPIRAL;
    }

    static double height(int tick, int bird) {
        if (tick >= SPIRAL) {
            return HOVER_Y + 0.25 * Math.sin(2 * Math.PI * (tick - SPIRAL) / 16 + bird * Math.PI / 2);
        }
        return START_Y + (HOVER_Y - START_Y) * tick / SPIRAL + (tick % 2 == 0 ? 0 : 0.18);
    }

    private static Vec place(Vec middle, double degrees, double radius, double up) {
        return middle.add(Kit.horizontal(degrees, radius)).add(0, up, 0);
    }

    record Bird(String texture, int wool) {
    }
}

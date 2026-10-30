package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class FrostAnimation implements BedAnimation {

    static final double[][] RINGS = {
        {3.00, 0.16}, {2.76, 0.43}, {2.54, 0.77}, {2.32, 1.13}, {2.12, 1.45}, {1.92, 1.72}, {1.73, 1.90},
        {1.55, 1.99}, {1.38, 1.99}, {1.23, 1.93}, {1.08, 1.81}, {0.94, 1.65}, {0.81, 1.47}, {0.69, 1.29},
        {0.58, 1.11}, {0.48, 0.94}, {0.39, 0.79}, {0.31, 0.66}, {0.235, 0.55}
    };
    static final int POINTS = 17;
    static final double POINT_STEP = 22;
    static final int FREEZE = 20;
    static final int ICE = 21;
    static final int SHATTER = 44;
    static final int SHARDS = 400;
    static final int SHATTER_TICKS = 3;

    @Override
    public String id() {
        return "friaca";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        Kit.BedDisplay display = Kit.bed(stage, bed);
        timeline.at(0, () -> stage.sound("fireworks.launch", middle, 1, 1));
        timeline.repeat(1, RINGS.length, 1, tick -> {
            for (int ring = 0; ring <= tick; ring++) {
                for (int point = 0; point < POINTS; point++) {
                    Vec at = middle.add(Kit.horizontal(point * POINT_STEP, RINGS[ring][0])).add(0, RINGS[ring][1], 0);
                    Kit.dust(stage, at, 0.90, 1.00, 1.00);
                }
            }
        });
        int ice = Kit.blockId(Material.ICE, 0);
        int packed = Kit.blockId(Material.PACKED_ICE, 0);
        timeline.at(FREEZE, () -> {
            display.remove();
            for (Vec half : List.of(bed.foot(), bed.head())) {
                stage.particle("BLOCK_CRACK", half.add(0, 0.3, 0), 30, new Vec(0.3, 0.2, 0.3), 0.05, ice);
                stage.sound("dig.glass", half, 1, 1);
            }
        });
        List<Actor> blocks = new ArrayList<>();
        timeline.at(ICE, () -> {
            for (Vec half : List.of(bed.foot(), bed.head())) {
                Vec at = half.add(0, -0.5, 0);
                Actor stand = stage.armorStand(at, 0, StandStyle.HIDDEN);
                Actor block = stage.fallingBlock(at, Material.PACKED_ICE, 0);
                block.ride(stand);
                blocks.add(block);
                blocks.add(stand);
            }
        });
        timeline.at(SHATTER, () -> {
            for (int glass = 0; glass < 10; glass++) {
                stage.sound("dig.glass", middle, 1, 1);
            }
        });
        timeline.repeat(SHATTER, SHATTER + SHATTER_TICKS - 1, 1, tick -> {
            int shards = SHARDS / SHATTER_TICKS + (tick < SHARDS % SHATTER_TICKS ? 1 : 0);
            for (int shard = 0; shard < shards; shard++) {
                stage.particle("BLOCK_DUST", middle.add(0, 0.5, 0), 0, shard(random), 1, shard % 2 == 0 ? ice : packed);
            }
        });
        timeline.at(SHATTER + 1, () -> blocks.forEach(Actor::remove));
    }

    static Vec shard(RandomGenerator random) {
        double length = Kit.between(random, 0.87, 1.13);
        double elevation = Math.asin(Kit.between(random, -0.1, 0.95));
        double azimuth = random.nextDouble() * 2 * Math.PI;
        return new Vec(Math.cos(azimuth) * Math.cos(elevation) * length, Math.sin(elevation) * length, Math.sin(azimuth) * Math.cos(elevation) * length);
    }
}

package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.random.RandomGenerator;

public final class EndermanAnimation implements BedAnimation {

    private static final int GRAB = 11;
    private static final int TELEPORT = 41;
    private static final int LEAVE = 47;
    private static final int CARRIED = 16;
    private static final int SCREAMING = 18;
    private static final short BED_HEAD = (short) (26 | 10 << 12);

    @Override
    public String id() {
        return "enderman";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec[] where = {bed.at(1, 0, 2)};
        Kit.flash(stage, timeline, 0, bed.middle().add(0, 1, 0), Kit.fading(Kit.LARGE_BALL, false, false, 0x9B30FF, 0x3D0A66));
        Kit.BedDisplay display = Kit.bed(stage, bed);
        Actor enderman = stage.mob("Enderman", where[0], where[0].yawTowards(bed.middle()));
        appear(stage, where[0], 1.05f);
        timeline.repeat(0, LEAVE, 1, tick -> stage.particle("PORTAL", where[0].add(0, 1.4, 0), 3, new Vec(0.3, 1, 0.3), 0.3));
        timeline.at(GRAB, () -> {
            display.remove();
            stage.particle("EXPLOSION_NORMAL", bed.middle(), 10, new Vec(0.6, 0.2, 0.6), 0.05);
            stage.sound("dig.wood", bed.middle(), 1, 0.79f);
            stage.sound("mob.endermen.scream", where[0], 0.8f, 1);
            stage.sound("mob.endermen.stare", where[0], 0.6f, 1);
            enderman.watch(CARRIED, BED_HEAD);
            enderman.watch(SCREAMING, (byte) 1);
        });
        timeline.at(GRAB + 1, () -> enderman.teleport(where[0], where[0].yawTowards(breaker), 0));
        timeline.at(TELEPORT, () -> {
            appear(stage, where[0], 1.05f);
            where[0] = where[0].add(bed.right().multiply(Kit.between(random, 4, 6))).add(bed.forward().multiply(Kit.between(random, 2, 4))).add(0, 1.5, 0);
            enderman.teleport(where[0], where[0].yawTowards(breaker), 0);
            appear(stage, where[0], 1.02f);
        });
        timeline.at(LEAVE, () -> {
            appear(stage, where[0], 0.95f);
            enderman.remove();
        });
    }

    private static void appear(Stage stage, Vec position, float pitch) {
        stage.particle("PORTAL", position.add(0, 1.2, 0), 40, new Vec(0.4, 1, 0.4), 1);
        stage.sound("mob.endermen.portal", position, 1, pitch);
    }
}

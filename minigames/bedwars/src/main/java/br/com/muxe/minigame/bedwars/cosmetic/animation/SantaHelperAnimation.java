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
import java.util.List;
import java.util.random.RandomGenerator;

public final class SantaHelperAnimation implements BedAnimation {

    static final List<String> PRESENTS = List.of(
        "dc2f5c5be6b575016773735974c9ca8807e3025e01b0e676ca7147e00545b510",
        "79af6ba11cad246cfdcfc2063d264c4e1738993138c3af3402f132373515195c",
        "143e0f73d8f447a573f33226fe4f9683b64dda42e7142c130b5b33c29f160183",
        "bcaad86c708eb27773a64f93479e39f0442a5ce086b63296c7b7d1cf51160956",
        "c1f23ce936d32c7cd2bb1cbd98a03458fffd530edaa373b882cc4ef664353fd4",
        "ed90816936569293a3824e6f1e7ea34a552fe90264b0352d914f8f645528b93d",
        "8c9fd224ee2dc7c84556c9c2e82a2d95d1ae355c79af7b5859b95138c25cf918",
        "d4b11b1d2fdd7dd8b89f6c9f732d5b7aa456a49bd156687bc8fe892b6dbfb20a",
        "edf633ee13bff8ec12d28f0162ed6a91d12ae145c253fd8d9bc13765cb7b00e3"
    );
    static final String ORNAMENT = "940da99ea4718907f17190eb15352c0da0de0cee186d4fabf6158f81926a504f";
    static final int FLIGHT = 88;
    static final int END = 99;
    static final double STEP_BACK = 0.35;
    static final double STEP_UP = 0.10;
    static final double[][] TRAIL = {{0.29, 0.65, 0.12}, {1, 1, 1}, {0.94, 0.20, 0.20}};

    @Override
    public String id() {
        return "ajudante-do-papai-noel";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Kit.flash(stage, timeline, 0, bed.foot(), Kit.burst(Kit.STAR, false, true, 0xB5251A, 0x2C7F28, Kit.WHITE));
        Kit.BedDisplay display = Kit.bed(stage, bed);
        List<Vec> corners = List.of(bed.at(1.34, -0.78, 0.69), bed.at(1.34, -0.78, -0.72), bed.at(-0.38, -0.78, 0.69), bed.at(-0.38, -0.78, -0.72));
        List<Actor> posts = new ArrayList<>();
        for (Vec corner : corners) {
            Actor post = stage.armorStand(corner, bed.yaw(), new StandStyle(true, true, false, false, false));
            post.pose(Pose.STANDING.withHead(new Vec(-10, 0, 0)));
            post.equipSkull(Slot.HELMET, ORNAMENT);
            posts.add(post);
        }
        timeline.repeat(1, FLIGHT, 1, tick -> {
            Vec offset = offset(bed, tick + 1);
            display.move(bed, offset);
            for (int index = 0; index < posts.size(); index++) {
                posts.get(index).teleport(corners.get(index).add(offset), bed.yaw(), 0);
            }
        });
        timeline.at(FLIGHT, () -> {
            display.remove();
            posts.forEach(Actor::remove);
        });
        timeline.repeat(0, END, 1, tick -> {
            double[] color = TRAIL[tick % TRAIL.length];
            Vec trail = offset(bed, Math.max(0, tick - 4));
            for (Vec corner : corners) {
                Vec at = corner.add(trail).add(0, 0.6, 0);
                Kit.dust(stage, at, color[0], color[1], color[2]);
                Kit.dust(stage, at, color[0], color[1], color[2]);
            }
        });
        List<Present> presents = new ArrayList<>();
        for (int index = 0; index < PRESENTS.size(); index++) {
            int drop = 9 + index * 10;
            String texture = PRESENTS.get(index);
            timeline.at(drop, () -> stage.sound("random.pop", bed.middle().add(offset(bed, drop)), 1, 1));
            timeline.at(drop + 1, () -> {
                Vec start = bed.middle().add(offset(bed, drop)).add(bed.forward()).add(0, -1.69, 0);
                Actor stand = stage.armorStand(start, bed.yaw(), new StandStyle(true, false, false, false, false));
                stand.equipSkull(Slot.HELMET, texture);
                presents.add(new Present(stand, start));
            });
        }
        timeline.repeat(10, END, 1, tick -> presents.forEach(present -> present.fall(stage)));
        timeline.at(END, () -> presents.forEach(present -> present.stand().remove()));
    }

    static Vec offset(BedFrame bed, int tick) {
        return bed.forward().multiply(-STEP_BACK * tick).add(0, STEP_UP * tick, 0);
    }

    static final class Present {

        private final Actor stand;
        private Vec position;
        private double speed;
        private boolean landed;

        Present(Actor stand, Vec position) {
            this.stand = stand;
            this.position = position;
        }

        Actor stand() {
            return stand;
        }

        void fall(Stage stage) {
            if (landed) {
                return;
            }
            speed = (speed + 0.08) * 0.98;
            Vec next = position.add(0, -speed, 0);
            if (stage.solid((int) Math.floor(next.x()), (int) Math.floor(next.y()), (int) Math.floor(next.z()))) {
                landed = true;
                next = new Vec(next.x(), Math.floor(next.y()) + 1, next.z());
            }
            position = next;
            stand.teleport(position, 0, 0);
        }
    }
}

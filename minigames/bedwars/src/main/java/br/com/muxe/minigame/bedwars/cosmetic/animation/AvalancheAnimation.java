package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class AvalancheAnimation implements BedAnimation {

    static final int SNOWBALLS = 30;
    static final int LIFETIME = 40;
    static final int SNOW_STAYS = 100;
    static final double GRAVITY = 0.03;
    static final double DRAG = 0.99;

    @Override
    public String id() {
        return "avalanche";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        timeline.at(0, () -> stage.particle("CLOUD", bed.middle(), 60, new Vec(1, 1, 1), 0));
        Vec start = bed.middle().add(0, 1, 0);
        for (int index = 0; index < SNOWBALLS; index++) {
            double angle = Math.toRadians(index * 12 + Kit.between(random, -15, 15));
            double speed = Kit.between(random, 0.15, 0.28);
            Vec velocity = new Vec(Math.cos(angle) * speed, Kit.between(random, 0.17, 0.32), Math.sin(angle) * speed);
            Flight flight = new Flight(start, velocity);
            timeline.at(0, () -> flight.ball = stage.snowball(start, velocity));
            timeline.repeat(1, LIFETIME, 1, tick -> flight.step(stage));
            timeline.at(LIFETIME, () -> flight.ball.remove());
        }
        timeline.lasts(SNOW_STAYS);
    }

    static final class Flight {

        private Vec position;
        private Vec velocity;
        private Actor ball;
        private boolean landed;

        Flight(Vec position, Vec velocity) {
            this.position = position;
            this.velocity = velocity;
        }

        void step(Stage stage) {
            if (landed) {
                return;
            }
            Vec next = position.add(velocity);
            int x = (int) Math.floor(next.x());
            int y = (int) Math.floor(next.y());
            int z = (int) Math.floor(next.z());
            if (stage.solid(x, y, z)) {
                landed = true;
                stage.sound("mob.chicken.plop", next, 1, 2);
                stage.block(x, y, z, Material.SNOW_BLOCK, 0);
                if (stage.solid(x, y - 1, z)) {
                    stage.block(x, y - 1, z, Material.SNOW_BLOCK, 0);
                }
                ball.remove();
                return;
            }
            position = next;
            velocity = new Vec(velocity.x() * DRAG, velocity.y() * DRAG - GRAVITY, velocity.z() * DRAG);
        }

        Vec position() {
            return position;
        }

        boolean landed() {
            return landed;
        }
    }
}

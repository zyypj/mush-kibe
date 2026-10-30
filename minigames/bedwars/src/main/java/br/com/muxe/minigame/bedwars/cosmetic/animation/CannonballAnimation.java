package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Actor;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.Slot;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.StandStyle;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.random.RandomGenerator;
import org.bukkit.Material;

public final class CannonballAnimation implements BedAnimation {

    static final String CANNONBALL = "18ab5f9ee9e2d3dc6667fe38b07eca344975409daef4fcde19814c563f41e776";
    static final int FALL = 22;
    static final double HEIGHT = 12.56;
    static final double DEPTH = 13.71;
    static final double HEAD = 1.6;
    static final int PIECES = 16;
    static final int DEBRIS = FALL + 1;
    static final int FLIGHT_LIMIT = 50;
    static final double FLOOR_MARGIN = 4;

    @Override
    public String id() {
        return "bola-de-canhao";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Vec middle = bed.middle();
        Kit.BedDisplay display = Kit.bed(stage, bed);
        Actor ball = stage.armorStand(ballAt(middle, 0), 0, StandStyle.HIDDEN);
        ball.equipSkull(Slot.HELMET, CANNONBALL);
        timeline.at(0, () -> stage.sound("fireworks.launch", middle, 1, 0.49f));
        timeline.repeat(1, FALL, 1, tick -> ball.teleport(ballAt(middle, tick), -12.5f * tick, 0));
        timeline.repeat(0, FALL - 1, 1, tick -> {
            Vec emitter = tick < 4 ? ballAt(middle, 0).add(0, HEAD + 0.5 * (tick + 1), 0) : ballAt(middle, tick - 4).add(0, HEAD, 0);
            stage.particle("SMOKE_LARGE", emitter, 2, new Vec(0.1, 0.3, 0.1), 0.01);
            stage.particle("FLAME", emitter, 1, new Vec(0.1, 0.2, 0.1), 0.01);
        });
        timeline.repeat(2, 20, 3, tick -> stage.sound("random.fizz", ballAt(middle, tick + 2).add(0, HEAD, 0), 0.3f, 0.63f + 0.05f * tick));
        timeline.at(FALL, () -> {
            display.remove();
            stage.particle("EXPLOSION_HUGE", middle.add(0, 0.3, 0), 1, Vec.ZERO, 0);
            stage.particle("SMOKE_LARGE", middle.add(0, 0.3, 0), 20, new Vec(0.8, 0.4, 0.8), 0.1);
            for (int flame = 0; flame < 50; flame++) {
                Vec direction = Kit.horizontal(random.nextDouble() * 360, Kit.between(random, 0.25, 0.5)).add(0, Kit.between(random, 0, 0.05), 0);
                stage.particle("FLAME", middle.add(0, 0.8, 0), 0, direction, 1);
            }
            stage.sound("random.explode", middle.add(0, 0.25, 0), 1, 0.79f);
            stage.sound("mob.zombie.woodbreak", middle.add(0, 0.25, 0), 0.6f, 0.79f);
            stage.sound("fire.ignite", middle.add(0, 0.25, 0), 1, 0.59f);
        });
        timeline.at(FALL + 2, ball::remove);
        for (int index = 0; index < PIECES; index++) {
            debris(middle, stage, timeline, random, index % 2 == 0 ? 14 : 0);
        }
    }

    static Vec ballAt(Vec middle, int tick) {
        return middle.add(0, HEIGHT - Kit.eased(tick, FALL, DEPTH), 0);
    }

    private static void debris(Vec middle, Stage stage, Timeline timeline, RandomGenerator random, int wool) {
        Vec start = middle.add(0, 0.78, 0);
        Vec velocity = Kit.horizontal(random.nextDouble() * 360, Kit.between(random, 0.07, 0.40)).add(0, Kit.between(random, 0.41, 0.75), 0);
        Body body = Body.item(start, velocity);
        Actor[] piece = new Actor[2];
        timeline.at(DEBRIS, () -> {
            piece[0] = stage.armorStand(start, 0, StandStyle.HIDDEN);
            piece[1] = stage.fallingBlock(start, Material.WOOL, wool);
            piece[1].ride(piece[0]);
        });
        timeline.repeat(DEBRIS + 1, DEBRIS + FLIGHT_LIMIT, 1, tick -> {
            if (piece[0].removed()) {
                return;
            }
            boolean landed = body.step(stage);
            Vec at = body.position();
            piece[0].teleport(at, 0, 0);
            if (landed) {
                stage.sound("dig.cloth", at, 0.7f, (float) Kit.between(random, 0.8, 1.17));
                stage.sound("step.cloth", at, 0.7f, (float) Kit.between(random, 0.6, 0.87));
                stage.sound("game.neutral.hurt.fall.small", at, 0.4f, (float) Kit.between(random, 0.8, 1.1));
                stage.particle("BLOCK_DUST", at.add(0, 0.5, 0), 12, new Vec(0.3, 0.3, 0.3), 0.1, Kit.blockId(Material.WOOL, wool));
            }
            if (landed || at.y() < middle.y() - FLOOR_MARGIN || tick == FLIGHT_LIMIT - 1) {
                piece[1].remove();
                piece[0].remove();
            }
        });
    }
}

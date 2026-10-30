package br.com.muxe.minigame.bedwars.cosmetic.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BedAnimationsTest {

    private static final BedFrame BED = BedFrame.of(6, 11, 17, 6, 16);
    private static final Vec BREAKER = new Vec(6.5, 11, 21.5);
    private static final int FLOOR = 10;

    @Test
    void everyAnimationEndsWithNothingLeftBehind() {
        for (BedAnimation animation : List.of(new EndermanAnimation(), new AvalancheAnimation(), new SaltAnimation(), new SantaHelperAnimation())) {
            RecordingStage stage = run(animation);

            assertThat(stage.alive()).as(animation.id()).isZero();
        }
    }

    @Test
    void theEndermanGrabsTheBedScreamsAndLeaves() {
        RecordingStage stage = run(new EndermanAnimation());

        RecordingStage.FakeActor enderman = stage.actors.stream().filter(actor -> actor.type.equals("Enderman")).findFirst().orElseThrow();
        assertThat(enderman.watched).containsEntry(16, (short) 40986).containsEntry(18, (byte) 1);
        assertThat(stage.sounds).contains("mob.endermen.scream", "dig.wood");
        assertThat(stage.actors).filteredOn(actor -> actor.type.startsWith("FallingBlock:BED_BLOCK")).extracting(actor -> actor.type)
            .containsExactly("FallingBlock:BED_BLOCK:2", "FallingBlock:BED_BLOCK:10");
    }

    @Test
    void avalancheSnowballsFallOnTheFloorAndTurnItIntoSnow() {
        RecordingStage stage = run(new AvalancheAnimation());

        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("Snowball")).hasSize(AvalancheAnimation.SNOWBALLS);
        assertThat(stage.blocks).isNotEmpty().allMatch(block -> block.endsWith("=SNOW_BLOCK"));
        assertThat(stage.sounds).contains("mob.chicken.plop");
    }

    @Test
    void theSaltShakerIsDrawnInDustAndShakesSideways() {
        RecordingStage stage = run(new SaltAnimation());

        assertThat(stage.particles.get("REDSTONE")).isGreaterThan(24 * 150);
        assertThat(SaltAnimation.SHAKER).allMatch(row -> row.length() == 13);
        assertThat(SaltAnimation.shake(5)).isZero();
        assertThat(Math.abs(SaltAnimation.shake(8))).isLessThanOrEqualTo(0.06);
        assertThat(stage.sounds).contains("dig.sand", "random.fizz", "game.neutral.hurt");
    }

    @Test
    void theSleighFliesAwayAndDropsNinePresents() {
        RecordingStage stage = run(new SantaHelperAnimation());

        long presents = stage.actors.stream().filter(actor -> actor.type.equals("ArmorStand")).count();
        assertThat(presents).isEqualTo(2 + 4 + SantaHelperAnimation.PRESENTS.size());
        Vec far = SantaHelperAnimation.offset(BED, SantaHelperAnimation.FLIGHT);
        assertThat(far.y()).isCloseTo(8.8, offset(0.01));
    }

    private static RecordingStage run(BedAnimation animation) {
        RecordingStage stage = new RecordingStage(block -> block[1] <= FLOOR);
        Timeline timeline = new Timeline();
        animation.script(BED, BREAKER, stage, timeline, new Random(42));
        for (int tick = 0; tick <= timeline.length(); tick++) {
            timeline.run(tick);
        }
        return stage;
    }
}

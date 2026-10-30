package br.com.muxe.minigame.bedwars.cosmetic.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BedAnimationsTest {

    private static final BedFrame BED = BedFrame.of(6, 11, 17, 6, 16);
    private static final Vec BREAKER = new Vec(6.5, 11, 21.5);
    private static final int FLOOR = 10;

    @Test
    void everyAnimationEndsWithNothingLeftBehind() {
        for (BedAnimation animation : new BedAnimations().all()) {
            RecordingStage stage = run(animation);

            assertThat(stage.alive()).as(animation.id()).isZero();
        }
    }

    @Test
    void thirteenRecordedAnimationsAreRegisteredByTheirCatalogIds() {
        assertThat(new BedAnimations().all()).extracting(BedAnimation::id).containsExactly(
            "ajudante-do-papai-noel", "avalanche", "coelhos", "passarinhos", "friaca", "fogos-de-artificio", "colheita-de-aboboras",
            "apavorante", "deserto", "tinta", "bola-de-canhao", "enderman", "sal-na-ferida");
        assertThat(new BedAnimations().find("tinta")).containsInstanceOf(PaintAnimation.class);
        assertThat(new BedAnimations().find("raio")).isEmpty();
    }

    @Test
    void paintDropsTheBucketOnTheBedAndSplatsFourColours() {
        RecordingStage stage = run(new PaintAnimation());

        assertThat(PaintAnimation.bucketAt(BED.middle(), PaintAnimation.FALL).y()).isCloseTo(BED.middle().y() - 0.9, offset(0.01));
        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("Firework")).hasSize(5);
        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("Item:STICK")).hasSize(4);
        assertThat(stage.blocks).isNotEmpty().allMatch(block -> block.endsWith("=WOOL") && block.contains(",10,"));
        assertThat(PaintAnimation.STAR).hasSize(22).allMatch(row -> row.length() == 21);
        assertThat(PaintAnimation.STAR.getFirst()).contains("#");
    }

    @Test
    void thePumpkinWobblesBurstsAndThrowsSixteenItemsUpAPentatonicScale() {
        RecordingStage stage = run(new PumpkinHarvestAnimation());

        assertThat(PumpkinHarvestAnimation.LANTERN).hasSize(11).allMatch(row -> row.length() == 13);
        assertThat(stage.actors).filteredOn(actor -> actor.type.startsWith("Item:")).hasSize(PumpkinHarvestAnimation.ITEMS);
        assertThat(PumpkinHarvestAnimation.pitch(0)).isEqualTo(0.5f);
        assertThat(PumpkinHarvestAnimation.pitch(15)).isEqualTo(2.0f);
        assertThat(PumpkinHarvestAnimation.high(2)).isTrue();
        assertThat(PumpkinHarvestAnimation.high(3)).isFalse();
        assertThat(stage.sounds).contains("note.bd", "random.explode", "dig.wood");
    }

    @Test
    void theBirdsSpiralInClimbAndBurstAboveTheBed() {
        RecordingStage stage = run(new BirdsAnimation());

        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("ArmorStand")).hasSize(4);
        assertThat(BirdsAnimation.radius(0)).isEqualTo(3.0);
        assertThat(BirdsAnimation.radius(BirdsAnimation.SPIRAL)).isEqualTo(1.0);
        assertThat(BirdsAnimation.height(BirdsAnimation.SPIRAL - 1, 0)).isGreaterThan(4.5);
        assertThat(stage.sounds).contains("mob.bat.idle", "mob.bat.death", "fireworks.launch");
    }

    @Test
    void theCannonballSmashesTheBedIntoSixteenWoolPieces() {
        RecordingStage stage = run(new CannonballAnimation());

        assertThat(stage.actors).filteredOn(actor -> actor.type.startsWith("FallingBlock:WOOL")).hasSize(CannonballAnimation.PIECES);
        assertThat(CannonballAnimation.ballAt(BED.middle(), CannonballAnimation.FALL).y()).isCloseTo(BED.middle().y() - 1.15, offset(0.01));
        assertThat(stage.sounds).contains("random.explode", "dig.cloth", "random.fizz");
    }

    @Test
    void frostBuildsTheDomeFreezesTheBedAndShattersIt() {
        RecordingStage stage = run(new FrostAnimation());

        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("FallingBlock:PACKED_ICE:0")).hasSize(2);
        assertThat(stage.particles.get("BLOCK_DUST")).isEqualTo(FrostAnimation.SHARDS);
        assertThat(stage.particles.get("REDSTONE")).isEqualTo(FrostAnimation.POINTS * 19 * 20 / 2);
        assertThat(stage.sounds).filteredOn(sound -> sound.equals("dig.glass")).hasSize(12);
    }

    @Test
    void theDesertCactusLiftsTheBedEightBlocksThenPopsEveryHead() {
        RecordingStage stage = run(new DesertAnimation());

        long heads = stage.actors.stream().filter(actor -> actor.type.equals("ArmorStand")).count();
        assertThat(heads).isEqualTo(2 + DesertAnimation.TRUNK + 7);
        assertThat(DesertAnimation.rise(DesertAnimation.RISE_UNTIL)).isCloseTo(8.0, offset(0.001));
        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("Item:CACTUS")).hasSize(DesertAnimation.TRUNK + 7);
        assertThat(stage.blocks).isNotEmpty().allMatch(block -> block.endsWith("=SAND") || block.endsWith("=SANDSTONE"));
        assertThat(stage.sounds).contains("creeper.primed", "ambient.weather.thunder", "random.explode");
    }

    @Test
    void fireworksPopOneRedBallOverTheBed() {
        RecordingStage stage = run(new FireworksAnimation());

        assertThat(stage.actors).extracting(actor -> actor.type).containsExactly("Firework");
        assertThat(stage.actors.getFirst().position).isEqualTo(BED.middle().add(0, 1, 0));
    }

    @Test
    void ghostsOrbitTheBedAndDiveIntoTheCauldron() {
        RecordingStage stage = run(new SpookyAnimation());

        assertThat(SpookyAnimation.radius(SpookyAnimation.EMERGE)).isEqualTo(1.5);
        assertThat(SpookyAnimation.radius(SpookyAnimation.SPLASH)).isZero();
        assertThat(SpookyAnimation.height(0, SpookyAnimation.SPLASH)).isCloseTo(-0.3, offset(0.001));
        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("FallingBlock:CAULDRON:3")).hasSize(1);
        assertThat(stage.sounds).contains("mob.ghast.moan", "random.splash", "liquid.lavapop");
    }

    @Test
    void rabbitsCarryTheBedInHopsThatLandBackOnTheFloor() {
        RecordingStage stage = run(new RabbitsAnimation());

        Vec travel = BED.forward().multiply(-1);
        assertThat(RabbitsAnimation.offset(travel, RabbitsAnimation.START + RabbitsAnimation.HOP).y()).isCloseTo(0, offset(0.001));
        assertThat(RabbitsAnimation.offset(travel, RabbitsAnimation.LAUNCH).z()).isCloseTo(3 * 4.14, offset(0.01));
        assertThat(RabbitsAnimation.offset(travel, RabbitsAnimation.END).y()).isGreaterThan(50);
        assertThat(stage.actors).filteredOn(actor -> actor.type.equals("Rabbit")).hasSize(4);
        assertThat(stage.sounds).filteredOn(sound -> sound.equals("step.grass")).hasSize(16);
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

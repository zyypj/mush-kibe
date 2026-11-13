package br.com.muxe.minigame.api.cosmetic.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.cosmetic.animation.Area;
import br.com.muxe.core.bukkit.cosmetic.animation.Scene;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage.BlockChange;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import br.com.muxe.minigame.api.cosmetic.animation.DesertVictory.CarpetFlight;
import br.com.muxe.minigame.api.cosmetic.animation.DesertVictory.Oasis;
import br.com.muxe.minigame.api.cosmetic.animation.DesertVictory.Placement;
import br.com.muxe.minigame.api.cosmetic.animation.RecordingStage.FakeMount;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class DesertVictoryTest {

    private static final Vec ORIGIN = new Vec(5.5, 27, -3.5);
    private static final Vec START_SEAT = new Vec(5.5, 28.78125, -3.5);
    private static final int ORIGIN_X = 5;
    private static final int ORIGIN_Z = -4;

    private static Scene scene() {
        return new Scene(ORIGIN, new Vec(0, 0, 1), Optional.empty(), List.of(UUID.randomUUID()));
    }

    private static RecordingStage stage() {
        return new RecordingStage(block -> block[1] < 27);
    }

    private static Timeline script(RecordingStage stage) {
        Timeline timeline = new Timeline();
        new DesertVictory().script(scene(), stage, timeline, new Random(5));
        return timeline;
    }

    private static void run(Timeline timeline, int from, int to) {
        for (int tick = from; tick <= to; tick++) {
            timeline.run(tick);
        }
    }

    private static long bulk(RecordingStage stage, Material material) {
        return stage.bulk.stream().filter(change -> change.material() == material).count();
    }

    private static long blocks(RecordingStage stage, Material material) {
        return stage.blocks.stream().filter(block -> block.endsWith("=" + material)).count();
    }

    private static long actors(RecordingStage stage, String type) {
        return stage.actors.stream().filter(actor -> actor.type.equals(type)).count();
    }

    private static long sounds(RecordingStage stage, String name) {
        return stage.sounds.stream().filter(name::equals).count();
    }

    @Test
    void itIsTheDesertoEffectAndLastsTwoHundredTicksWithoutTouchingTheTime() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        run(timeline, 0, timeline.length());

        assertThat(new DesertVictory().id()).isEqualTo("deserto");
        assertThat(timeline.length()).isEqualTo(200);
        assertThat(stage.times).isEmpty();
    }

    @Test
    void theWaveConvertsOneHundredTwentyEightColumnsPerTickFromTickTwoOutwardsByManhattanDistance() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        run(timeline, 0, 1);
        assertThat(stage.bulk).isEmpty();
        int previousFarthest = 0;
        for (int tick = 2; tick <= 6; tick++) {
            int before = stage.bulk.size();
            timeline.run(tick);
            Set<String> columns = new HashSet<>();
            int nearest = Integer.MAX_VALUE;
            int farthest = 0;
            for (BlockChange change : stage.bulk.subList(before, stage.bulk.size())) {
                columns.add(change.x() + "," + change.z());
                int distance = DesertRules.distance(change.x(), change.z(), ORIGIN_X, ORIGIN_Z);
                nearest = Math.min(nearest, distance);
                farthest = Math.max(farthest, distance);
            }
            assertThat(columns).as("tick %d", tick).hasSize(128);
            assertThat(nearest).as("tick %d", tick).isGreaterThanOrEqualTo(previousFarthest);
            previousFarthest = farthest;
        }
    }

    @Test
    void theColumnUnderTheWinnerIsConvertedFromTheFirstTickByAbsoluteHeightWithSandOnTop() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        run(timeline, 0, 2);

        List<BlockChange> column = stage.bulk.stream().filter(change -> change.x() == ORIGIN_X && change.z() == ORIGIN_Z).toList();
        assertThat(column).hasSize(12);
        BlockChange top = column.stream().filter(change -> change.y() == 26).findFirst().orElseThrow();
        assertThat(top.material()).isEqualTo(Material.SAND);
        boolean clay = top.data() == 1;
        for (BlockChange change : column) {
            assertThat(change.y()).isBetween(15, 26);
            if (change.y() < 26) {
                assertThat(change.material()).isEqualTo(DesertRules.stratum(change.y(), clay).material());
                assertThat(change.data()).isEqualTo(DesertRules.stratum(change.y(), clay).data());
            }
        }
    }

    @Test
    void theOasisPartitionsItsDiscIntoRingsWithPondsOnlyInTheInnerOnes() {
        Oasis oasis = new Oasis(0, 27, 0, new Random(3));

        int expected = 0;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                expected += Math.hypot(dx, dz) < 9 ? 1 : 0;
            }
        }
        assertThat(oasis.columns()).hasSize(expected);
        assertThat(oasis.ring(0)).hasSize(1);
        for (int ring = 0; ring <= 8; ring++) {
            assertThat(oasis.ring(ring)).allSatisfy(column -> assertThat((int) Math.floor(Math.hypot(column.x(), column.z()))).isEqualTo(column.ring()));
        }
        assertThat(oasis.columns()).filteredOn(column -> column.surface() == Oasis.Surface.WATER).isNotEmpty().allSatisfy(column -> {
            assertThat(column.distance()).isLessThan(3.9);
            assertThat(column.ring()).isBetween(1, 3);
        });
        assertThat(oasis.columns()).filteredOn(column -> column.distance() > 0 && column.distance() <= 2.24)
            .allSatisfy(column -> assertThat(column.surface()).isEqualTo(Oasis.Surface.WATER));
        assertThat(oasis.columns()).filteredOn(column -> column.distance() >= 6.4 && column.ring() > 0)
            .allSatisfy(column -> assertThat(column.surface()).isEqualTo(Oasis.Surface.SAND));
        assertThat(oasis.columns()).filteredOn(column -> column.distance() > 0 && column.distance() <= 4.5)
            .filteredOn(column -> column.surface() != Oasis.Surface.WATER)
            .allSatisfy(column -> assertThat(column.surface()).isEqualTo(Oasis.Surface.GRASS));
    }

    @Test
    void ringsAreBuiltEveryTwoTicksFromTickTenAndThePondFillsInsideOut() {
        assertThat(Oasis.ringTick(0)).isEqualTo(10);
        assertThat(Oasis.ringTick(1)).isEqualTo(12);
        assertThat(Oasis.ringTick(8)).isEqualTo(26);
        assertThat(Oasis.pondTick(1)).isEqualTo(24);
        assertThat(Oasis.pondTick(2)).isEqualTo(26);
        assertThat(Oasis.pondTick(3)).isEqualTo(28);
    }

    @Test
    void thePalmsHaveSixOrSevenJungleLogsAndTwentyOneLeavesEach() {
        Oasis oasis = new Oasis(0, 27, 0, new Random(3));

        List<Placement> logs = oasis.placements().stream().filter(placement -> placement.material() == Material.LOG).toList();
        List<Placement> leaves = oasis.placements().stream().filter(placement -> placement.material() == Material.LEAVES).toList();
        assertThat(logs.size()).isBetween(18, 21);
        assertThat(logs).allSatisfy(log -> assertThat(log.data()).isEqualTo(3));
        assertThat(logs.stream().mapToInt(Placement::tick).min().orElseThrow()).isEqualTo(33);
        assertThat(leaves).hasSize(63);
        assertThat(leaves).allSatisfy(leaf -> assertThat(leaf.data()).isEqualTo(7));
        assertThat(oasis.placements().stream().filter(placement -> placement.material() == Material.WATER_LILY).count()).isEqualTo(3);
    }

    @Test
    void theOasisRisesFromTheGoldPillarOutwardsThenGrowsPondPalmsAndPlants() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        run(timeline, 0, 9);
        assertThat(bulk(stage, Material.GOLD_BLOCK)).isZero();
        timeline.run(10);
        assertThat(bulk(stage, Material.GOLD_BLOCK)).isEqualTo(5);
        run(timeline, 11, 23);
        assertThat(bulk(stage, Material.STATIONARY_WATER)).isZero();
        timeline.run(24);
        assertThat(bulk(stage, Material.STATIONARY_WATER)).isPositive();
        run(timeline, 25, 32);
        assertThat(blocks(stage, Material.LOG)).isZero();
        timeline.run(33);
        assertThat(blocks(stage, Material.LOG)).isEqualTo(3);

        run(timeline, 34, 200);

        assertThat(blocks(stage, Material.LOG)).isBetween(18L, 21L);
        assertThat(blocks(stage, Material.LEAVES)).isEqualTo(63);
        assertThat(blocks(stage, Material.WATER_LILY)).isEqualTo(3);
        assertThat(blocks(stage, Material.SUGAR_CANE_BLOCK)).isBetween(14L, 21L);
        assertThat(blocks(stage, Material.LONG_GRASS)).isBetween(19L, 25L);
        assertThat(sounds(stage, "dig.sand")).isEqualTo(9);
        assertThat(sounds(stage, "random.splash")).isEqualTo(4);
        assertThat(sounds(stage, "random.levelup")).isEqualTo(1);
        assertThat(sounds(stage, "liquid.water")).isEqualTo(8);
        assertThat(stage.particles.get("WATER_SPLASH")).isEqualTo(80);
    }

    @Test
    void theCarpetIsFifteenMarkerStandsInThreeColoursWithTheRipple() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        timeline.run(0);

        assertThat(actors(stage, "ArmorStand")).isEqualTo(15);
        assertThat(stage.actors.stream().map(actor -> actor.position).distinct().count()).isEqualTo(15);
        long yellow = 0;
        long purple = 0;
        long red = 0;
        for (int stand = 0; stand < 15; stand++) {
            int color = DesertVictory.carpetColor(stand);
            yellow += color == 4 ? 1 : 0;
            purple += color == 10 ? 1 : 0;
            red += color == 14 ? 1 : 0;
        }
        assertThat(yellow).isEqualTo(12);
        assertThat(purple).isEqualTo(2);
        assertThat(red).isEqualTo(1);
        assertThat(DesertVictory.carpetColor(7)).isEqualTo(14);
        for (int tick = 0; tick < 60; tick++) {
            for (int row = -2; row <= 2; row++) {
                assertThat(DesertVictory.ripple(tick, row * 0.625)).isBetween(-1.28, -1.13);
            }
        }
    }

    @Test
    void theCarpetRisesAboutTwoAndAHalfBlocksAndHoversWithoutW() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        timeline.run(0);
        FakeMount mount = stage.mounts.getFirst();
        mount.yaw = 90;
        mount.pitch = 30;
        run(timeline, 1, 40);

        assertThat(mount.seat().x()).isEqualTo(START_SEAT.x());
        assertThat(mount.seat().z()).isEqualTo(START_SEAT.z());
        assertThat(mount.seat().y() - START_SEAT.y()).isBetween(2.3, 2.46);
    }

    @Test
    void onlyHoldingWMovesTheCarpetAlongTheLookDirectionAndReleasingItCoastsToAStop() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        timeline.run(0);
        FakeMount mount = stage.mounts.getFirst();
        run(timeline, 1, 20);
        double before = mount.seat().z();
        mount.pushing = true;
        run(timeline, 21, 50);
        assertThat(mount.seat().z() - before).isGreaterThan(15);
        assertThat(Math.abs(mount.seat().x() - START_SEAT.x())).isLessThan(1e-6);

        mount.pushing = false;
        run(timeline, 51, 100);
        Vec parked = mount.seat();
        run(timeline, 101, 110);
        assertThat(mount.seat().distance(parked)).isLessThan(0.05);
    }

    @Test
    void theFlightEasesTheHeadingByTwentyOnePercentAndTheSpeedTowardsTopSpeed() {
        CarpetFlight flight = new CarpetFlight(new Vec(0, 10, 0), 0);

        flight.step(true, 100, 0);

        assertThat(flight.heading()).isCloseTo(21, within(1e-9));
        assertThat(flight.speed()).isCloseTo(0.15 * 0.98, within(1e-9));
        for (int tick = 0; tick < 60; tick++) {
            flight.step(true, 100, 0);
        }
        assertThat(flight.speed()).isBetween(0.97, 0.98);
        double speed = flight.speed();
        flight.step(false, 100, 0);
        assertThat(flight.speed()).isCloseTo(speed * 0.85, within(1e-9));
    }

    @Test
    void theFlightNeverSinksBelowItsFloorEvenLookingStraightDown() {
        CarpetFlight flight = new CarpetFlight(new Vec(0, 10, 0), 0);

        for (int tick = 0; tick < 100; tick++) {
            flight.step(true, 0, 90);
            assertThat(flight.position().y()).isGreaterThanOrEqualTo(flight.floor());
        }

        assertThat(flight.floor()).isEqualTo(10 - 0.72);
        assertThat(flight.position().y()).isEqualTo(flight.floor());
    }

    @Test
    void lookingUpClimbsAndStrafingIsIgnored() {
        CarpetFlight flight = new CarpetFlight(new Vec(0, 10, 0), 0);
        for (int tick = 0; tick < 40; tick++) {
            flight.step(true, 0, -45);
        }

        assertThat(flight.position().y()).isGreaterThan(20);
        assertThat(flight.position().z()).isGreaterThan(10);
    }

    @Test
    void theWholeTimelineRainsGoldPlaysTheSoundsLeavesATrailAndCleansUp() {
        RecordingStage stage = stage();
        Timeline timeline = script(stage);

        for (int tick = 0; tick <= timeline.length(); tick++) {
            timeline.run(tick);
            if (tick == 0) {
                continue;
            }
            FakeMount mount = stage.mounts.getFirst();
            mount.pushing = tick > 20 && tick < 140;
            mount.yaw = tick * 3f;
            mount.pitch = (tick % 40) - 20;
        }

        assertThat(sounds(stage, "fireworks.launch")).isEqualTo(20);
        assertThat(sounds(stage, "mob.enderdragon.wings")).isEqualTo(4);
        assertThat(sounds(stage, "random.orb")).isBetween(5L, 100L);
        assertThat(actors(stage, "Item:GOLD_BLOCK")).isEqualTo(50);
        assertThat(actors(stage, "Item:GOLD_INGOT")).isEqualTo(100);
        assertThat(actors(stage, "Item:GOLDEN_APPLE")).isEqualTo(50);
        assertThat(actors(stage, "Item:GOLD_NUGGET")).isBetween(110L, 176L);
        assertThat(stage.particles.get("REDSTONE")).isGreaterThan(100);
        assertThat(stage.particles.get("FIREWORKS_SPARK")).isGreaterThan(50);
        assertThat(bulk(stage, Material.SAND)).isPositive();
        assertThat(stage.mounts).hasSize(1);
        assertThat(stage.times).isEmpty();
        assertThat(stage.alive()).isZero();
    }

    @Test
    void goldRainsAllOverTheMapAboveTheCarpet() {
        Area map = new Area(-80, -80, 80, 80);
        Vec vehicle = new Vec(5, 70, 5);
        Random random = new Random(9);
        for (int drop = 0; drop < 300; drop++) {
            Vec spot = DesertVictory.rainSpot(map, vehicle, random);
            assertThat(spot.x()).isBetween(-80.0, 80.0);
            assertThat(spot.z()).isBetween(-80.0, 80.0);
            assertThat(spot.y() - vehicle.y()).isBetween(DesertVictory.RAIN_MIN_HEIGHT, DesertVictory.RAIN_MAX_HEIGHT);
        }
    }

    @Test
    void theOasisAlwaysRisesTenBlocksWhereTheWinnerLooksEvenInsideABuilding() {
        assertThat(DesertVictory.oasisCentre(0, 0, new Vec(0, 0, 1), List.of())).containsExactly(0, 10);
        assertThat(DesertVictory.oasisCentre(0, 0, new Vec(-1, 0, 0), List.of())).containsExactly(-10, 0);
        assertThat(DesertVictory.oasisCentre(5, -3, new Vec(1, 0, 1), List.of())).containsExactly(12, 4);
    }

    @Test
    void aSecondOasisGoesFurtherAlongTheLookInsteadOfOverlappingTheFirst() {
        List<int[]> first = List.<int[]>of(new int[] {0, 10});

        assertThat(DesertVictory.oasisCentre(0, 0, new Vec(0, 0, 1), first)).containsExactly(0, 28);
        assertThat(DesertVictory.oasisCentre(60, 0, new Vec(1, 0, 0), first)).containsExactly(70, 0);
    }

    @Test
    void onAMatchTheWholeMapTurnsToDesert() {
        RecordingStage stage = stage();
        Timeline timeline = new Timeline();
        Scene scene = new Scene(ORIGIN, new Vec(0, 0, 1), Optional.empty(), List.of(UUID.randomUUID()), new Area(-140, -140, 140, 140));
        new DesertVictory().script(scene, stage, timeline, new Random(5));
        run(timeline, 0, DesertVictory.WAVE_FIRST_TICK + DesertRules.WAVE_TICKS);

        assertThat(stage.bulk).anyMatch(change -> change.x() <= -130 && change.z() >= 130);
        assertThat(stage.bulk).anyMatch(change -> change.x() >= 130 && change.z() <= -130);
    }
}

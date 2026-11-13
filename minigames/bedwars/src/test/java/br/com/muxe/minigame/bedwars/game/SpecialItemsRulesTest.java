package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.game.Rewind.Sample;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SpecialItemsRulesTest {

    private static final double ICE_X = -22.4031878052034;
    private static final double ICE_Y = 31.0;
    private static final double ICE_Z = 71.27010615431317;
    private static final float ICE_YAW = 129.5595f;
    private static final String RECORDED_ICE = "-24,71 -24,70 -23,69 -25,70 -24,69 -26,70 -25,69 -25,68 -27,69 -26,68 -25,67 -26,69 -27,68 -26,67 -28,68 -28,67 "
        + "-27,66 -27,67 -29,67 -28,66 -30,66 -29,66 -28,65 -30,65 -29,64 -29,65 -31,65 -31,64 -30,64 -31,63 -32,65 -32,63 -32,62 -32,64 -33,64 -34,63 -33,62 "
        + "-33,63 -34,62 -33,61 -35,62 -34,61 -34,60 -36,61 -35,61 -35,60 -37,61 -36,60 -35,59 -37,60 -37,59 -36,59 -38,59 -37,58 -39,59 -38,58 -38,57 -40,58 "
        + "-39,57 -39,58 -40,57 -39,56 -41,57 -41,56 -40,55 -40,56 -42,56 -41,55 -43,56 -42,55 -42,54 -44,55 -43,54 -42,53 -43,55 -44,54 -43,53 -45,54 -44,53 "
        + "-44,52 -46,53 -45,52 -45,53 -47,52 -46,52 -45,51";

    @Test
    void theIceBridgeIsTheRecordedOneThirtyBlocksAheadUnderTheFeet() {
        Set<Cell> recorded = new HashSet<>();
        Arrays.stream(RECORDED_ICE.split(" ")).map(pair -> pair.split(",")).forEach(pair -> recorded.add(new Cell(Integer.parseInt(pair[0]), 30, Integer.parseInt(pair[1]))));
        Set<Cell> built = new HashSet<>();
        IntStream.rangeClosed(0, IceBridge.LAST_TICK + 4).forEach(tick -> built.addAll(IceBridge.at(tick, ICE_X, ICE_Y, ICE_Z, ICE_YAW)));

        assertThat(recorded).hasSize(86);
        assertThat(built).containsAll(recorded);
        assertThat(built.size() - recorded.size()).isLessThanOrEqualTo(3);
        assertThat(IceBridge.at(2, ICE_X, ICE_Y, ICE_Z, ICE_YAW)).contains(new Cell(-24, 30, 71), new Cell(-24, 30, 70), new Cell(-23, 30, 69));
        assertThat(IceBridge.at(1, ICE_X, ICE_Y, ICE_Z, ICE_YAW)).isEmpty();
        assertThat(IceBridge.at(IceBridge.LAST_TICK + 2, ICE_X, ICE_Y, ICE_Z, ICE_YAW)).isEmpty();
        assertThat(IceBridge.LAST_TICK).isEqualTo(60);
    }

    @Test
    void theBridgeEggLaysAPlusOfWoolTwoBlocksUnderItselfUpToTwentyFiveBlocksAway() {
        assertThat(BridgeEgg.under(-10.62, 33.12, 63.62)).containsExactly(new Cell(-11, 31, 63), new Cell(-10, 31, 63), new Cell(-12, 31, 63), new Cell(-11, 31, 64),
            new Cell(-11, 31, 62));
        assertThat(BridgeEgg.builds(-8.81, 32.5, 67.59, -18.5, 32.5, 46.5, 18)).isTrue();
        assertThat(BridgeEgg.builds(-8.81, 32.5, 67.59, -20.69, 33.38, 42.16, 21)).isFalse();
        assertThat(BridgeEgg.builds(0, 30, 0, 0, 32, 1, BridgeEgg.MAX_TICKS + 1)).isFalse();
    }

    @Test
    void aSpiderShotWebsTheBlockItLandsInAndItsFourSides() {
        assertThat(SpiderWeb.webs(-5.2, 32.4, 69.9)).containsExactly(new Cell(-6, 32, 69), new Cell(-5, 32, 69), new Cell(-7, 32, 69), new Cell(-6, 32, 70),
            new Cell(-6, 32, 68));
        assertThat(SpiderWeb.COOLDOWN.toSeconds()).isEqualTo(7);
        assertThat(SpiderWeb.WEB_TICKS).isEqualTo(140);
    }

    @Test
    void theParachuteOpensFiveBlocksUnderWhereItWasUsedWithTheWoolInTheMiddle() {
        int platform = Parachute.platformY(31.785);

        assertThat(platform).isEqualTo(26);
        assertThat(Parachute.reached(26.561, platform)).isFalse();
        assertThat(Parachute.reached(25.655, platform)).isTrue();
        List<Cell> cells = Parachute.platform(-25.65, platform, 72.03);
        assertThat(cells).hasSize(9).first().isEqualTo(new Cell(-26, 26, 72));
        assertThat(cells).contains(new Cell(-27, 26, 71), new Cell(-25, 26, 73), new Cell(-25, 26, 71), new Cell(-27, 26, 73));
    }

    @Test
    void rewindingGoesBackThreeSecondsTakingEveryOtherTickNewestFirst() {
        Rewind rewind = new Rewind();
        for (int tick = 0; tick <= 100; tick++) {
            rewind.record(new Sample(tick * 0.25, 31, 0, tick, 0));
        }

        List<Sample> trail = rewind.trail();

        assertThat(trail).hasSize(30);
        assertThat(trail.getFirst().yaw()).isEqualTo(98f);
        assertThat(trail.get(1).yaw()).isEqualTo(96f);
        assertThat(trail.getLast().yaw()).isEqualTo(40f);
    }

    @Test
    void aTeleportForgetsTheWayBeforeItSoRewindingNeverCrossesARespawn() {
        Rewind rewind = new Rewind();
        for (int tick = 0; tick < 20; tick++) {
            rewind.record(new Sample(tick * 0.25, 31, 0, tick, 0));
        }
        for (int tick = 20; tick < 30; tick++) {
            rewind.record(new Sample(100 + tick * 0.25, 31, 0, tick, 0));
        }

        assertThat(rewind.trail()).extracting(Sample::yaw).containsExactly(27f, 25f, 23f, 21f);
        rewind.clear();
        assertThat(rewind.trail()).isEmpty();
    }
}

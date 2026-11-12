package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.game.CompactTower.Piece;
import br.com.muxe.minigame.bedwars.game.CompactTower.Placed;
import br.com.muxe.minigame.bedwars.game.ForgeLayout.Facing;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CompactTowerTest {

    @Test
    void theTowerIsTheRecordedHundredAndFourWoolAndSixLaddersOverTwentyTwoTicks() {
        assertThat(CompactTower.PIECES).hasSize(110);
        assertThat(CompactTower.PIECES).filteredOn(Piece::ladder).hasSize(6);
        assertThat(CompactTower.LAST_TICK).isEqualTo(21);
        assertThat(CompactTower.PIECES).extracting(piece -> List.of(piece.side(), piece.up(), piece.ahead())).doesNotHaveDuplicates();
    }

    @Test
    void facingEastTheFirstLayerLandsWhereTheRecordingPutIt() {
        Set<List<Integer>> first = CompactTower.at(0, 0, 0, 0, Facing.EAST).stream()
            .map(placed -> List.of(placed.x(), placed.y(), placed.z())).collect(Collectors.toSet());

        assertThat(first).containsExactlyInAnyOrder(List.of(-1, 0, -1), List.of(-1, 0, 1), List.of(0, 0, -2), List.of(0, 0, 2), List.of(1, 0, -2),
            List.of(1, 0, 0), List.of(1, 0, 2), List.of(2, 0, -1), List.of(2, 0, 0), List.of(2, 0, 1));
        assertThat(CompactTower.at(0, 0, 0, 0, Facing.EAST)).filteredOn(Placed::ladder).extracting(Placed::x).containsExactly(1);
    }

    @Test
    void theEntranceFacesThePlayerAndTheTowerTurnsWithTheirLook() {
        List<Placed> east = everyPiece(Facing.EAST);
        List<Placed> west = everyPiece(Facing.WEST);
        List<Placed> south = everyPiece(Facing.SOUTH);

        assertThat(east).noneMatch(placed -> placed.x() == -1 && placed.z() == 0 && placed.y() <= 2);
        assertThat(south).noneMatch(placed -> placed.x() == 0 && placed.z() == -1 && placed.y() <= 2);
        for (int index = 0; index < east.size(); index++) {
            assertThat(west.get(index)).isEqualTo(new Placed(-east.get(index).x(), east.get(index).y(), -east.get(index).z(), east.get(index).ladder()));
        }
    }

    @Test
    void theLadderLeansOnTheFarWallAndTheFacingComesFromTheLook() {
        assertThat(CompactTower.ladderData(Facing.EAST)).isEqualTo((byte) 4);
        assertThat(CompactTower.ladderData(Facing.WEST)).isEqualTo((byte) 5);
        assertThat(CompactTower.ladderData(Facing.SOUTH)).isEqualTo((byte) 2);
        assertThat(CompactTower.ladderData(Facing.NORTH)).isEqualTo((byte) 3);
        assertThat(CompactTower.facing(267.8f)).isEqualTo(Facing.EAST);
        assertThat(CompactTower.facing(0f)).isEqualTo(Facing.SOUTH);
        assertThat(CompactTower.facing(90f)).isEqualTo(Facing.WEST);
        assertThat(CompactTower.facing(-180f)).isEqualTo(Facing.NORTH);
    }

    @Test
    void anotherTowerWaitsFifteenSecondsShownToATenthRoundedUp() {
        Cooldowns cooldowns = new Cooldowns(CompactTower.COOLDOWN);
        UUID ana = UUID.randomUUID();
        Instant placed = Instant.parse("2026-11-12T02:31:15Z");

        assertThat(cooldowns.remaining(ana, placed)).isEmpty();
        cooldowns.start(ana, placed);

        assertThat(cooldowns.remaining(ana, placed.plusMillis(2380)).map(left -> Cooldowns.seconds(left, ","))).contains("12,7");
        assertThat(cooldowns.remaining(ana, placed.plusMillis(13950)).map(left -> Cooldowns.seconds(left, ","))).contains("1,1");
        assertThat(cooldowns.remaining(ana, placed.plus(Duration.ofSeconds(15)))).isEmpty();
    }

    private static List<Placed> everyPiece(Facing facing) {
        return IntStream.rangeClosed(0, CompactTower.LAST_TICK).boxed().flatMap(tick -> CompactTower.at(tick, 0, 0, 0, facing).stream()).toList();
    }
}

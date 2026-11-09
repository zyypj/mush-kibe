package br.com.muxe.minigame.api.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.network.ArenaInfo;
import br.com.muxe.minigame.api.network.RoomInfo;
import br.com.muxe.minigame.api.network.RoomsPacket;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchmakerTest {

    private static final ArenaInfo LIGHTHOUSE_SOLO = new ArenaInfo("solo", "farol", "Farol");
    private static final ArenaInfo CANYON_SOLO = new ArenaInfo("solo", "canion", "Cânion");
    private static final ArenaInfo CANYON_DUEL = new ArenaInfo("1v1", "canion-1v1", "Cânion");

    @Test
    void theFullestOpenRoomOfTheModeWins() {
        RoomsPacket one = new RoomsPacket("bedwars-1", List.of(new RoomInfo("aaaaaa", "solo", "farol", true, 3, 8), new RoomInfo("bbbbbb", "solo", "canion", true, 7, 8)), List.of(LIGHTHOUSE_SOLO, CANYON_SOLO));
        RoomsPacket two = new RoomsPacket("bedwars-2", List.of(new RoomInfo("cccccc", "solo", "farol", true, 5, 8)), List.of(LIGHTHOUSE_SOLO));

        assertThat(Matchmaker.plan(List.of(one, two), "solo", Optional.empty())).contains(new Matchmaker.Target("bedwars-1", "bbbbbb"));
        assertThat(Matchmaker.plan(List.of(one, two), "solo", Optional.of("farol"))).contains(new Matchmaker.Target("bedwars-2", "cccccc"));
    }

    @Test
    void fullStartedOrOtherModeRoomsAreSkippedAndANewRoomIsAskedFromTheEmptiestServer() {
        RoomsPacket one = new RoomsPacket("bedwars-1", List.of(new RoomInfo("aaaaaa", "solo", "farol", true, 8, 8), new RoomInfo("bbbbbb", "solo", "farol", false, 2, 8)), List.of(LIGHTHOUSE_SOLO));
        RoomsPacket two = new RoomsPacket("bedwars-2", List.of(new RoomInfo("cccccc", "1v1", "canion-1v1", true, 1, 2)), List.of(LIGHTHOUSE_SOLO, CANYON_DUEL));

        assertThat(Matchmaker.plan(List.of(one, two), "solo", Optional.empty())).contains(new Matchmaker.Target("bedwars-2", ""));
        assertThat(Matchmaker.plan(List.of(one, two), "solo", Optional.of("canion"))).isEmpty();
    }

    @Test
    void aPartyOnlyGoesToARoomWithASeatForEveryMember() {
        RoomsPacket one = new RoomsPacket("bedwars-1", List.of(new RoomInfo("aaaaaa", "solo", "farol", true, 6, 8), new RoomInfo("bbbbbb", "solo", "farol", true, 4, 8)), List.of(LIGHTHOUSE_SOLO));
        RoomsPacket two = new RoomsPacket("bedwars-2", List.of(new RoomInfo("cccccc", "solo", "farol", true, 7, 8)), List.of(LIGHTHOUSE_SOLO));

        assertThat(Matchmaker.plan(List.of(one, two), "solo", Optional.empty(), 3)).contains(new Matchmaker.Target("bedwars-1", "bbbbbb"));
        assertThat(Matchmaker.plan(List.of(one, two), "solo", Optional.empty(), 5)).contains(new Matchmaker.Target("bedwars-2", ""));
        assertThat(RoomChoice.pick(one.rooms(), "solo", Optional.empty(), Optional.of("aaaaaa"), 3)).map(RoomInfo::code).contains("bbbbbb");
        assertThat(new RoomInfo("aaaaaa", "solo", "farol", true, 6, 8).hasRoom(2)).isTrue();
        assertThat(new RoomInfo("aaaaaa", "solo", "farol", true, 6, 8).hasRoom(3)).isFalse();
    }

    @Test
    void aWantedRoomIsKeptWhileItHasSpace() {
        List<RoomInfo> rooms = List.of(new RoomInfo("aaaaaa", "solo", "farol", true, 2, 8), new RoomInfo("bbbbbb", "solo", "farol", true, 6, 8));

        assertThat(RoomChoice.pick(rooms, "solo", Optional.empty(), Optional.of("aaaaaa"))).map(RoomInfo::code).contains("aaaaaa");
        assertThat(RoomChoice.pick(rooms, "solo", Optional.empty(), Optional.of("zzzzzz"))).map(RoomInfo::code).contains("bbbbbb");
    }

    @Test
    void theDirectoryForgetsSilentServersAndCountsRoomsPerMap() {
        MutableClock clock = MutableClock.startingAt("2026-11-02T12:00:00Z");
        RoomDirectory directory = new RoomDirectory(clock, Duration.ofSeconds(10));
        directory.update(new RoomsPacket("bedwars-1", List.of(new RoomInfo("aaaaaa", "solo", "farol", true, 3, 8), new RoomInfo("bbbbbb", "solo", "farol", false, 8, 8)), List.of(LIGHTHOUSE_SOLO, CANYON_SOLO)));

        assertThat(directory.playersIn("solo")).isEqualTo(11);
        assertThat(directory.rooms("solo", "farol")).isEqualTo(new RoomDirectory.RoomCount(1, 2));
        assertThat(directory.arenas("solo")).containsExactly(LIGHTHOUSE_SOLO, CANYON_SOLO);

        clock.advance(Duration.ofSeconds(11));

        assertThat(directory.alive()).isEmpty();
        assertThat(directory.playersIn("solo")).isZero();
    }
}

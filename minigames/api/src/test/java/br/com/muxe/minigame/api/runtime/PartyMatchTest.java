package br.com.muxe.minigame.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.match.TestArena;
import br.com.muxe.minigame.api.network.Reserved;
import br.com.muxe.minigame.engine.arena.ArenaCatalog;
import br.com.muxe.minigame.engine.arena.ArenaStore;
import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.ArenaFiles.StoredArena;
import br.com.muxe.minigame.engine.team.PartyGroups;
import br.com.muxe.minigame.engine.world.cell.Cell;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartyMatchTest {

    private static final GameMode ONE_V_ONE = new GameMode("1v1", "1v1", 2, 2, 1);
    private static final GameMode TWO_V_TWO = new GameMode("2v2", "2v2", 4, 4, 2);
    private static final String WARNING = "AVISO Esta partida não conta estatísticas.";

    private final MutableClock clock = MutableClock.startingAt("2026-11-09T20:00:00Z");
    private final FakeStage stage = new FakeStage();
    private final UUID ana = id("ana");
    private final UUID bia = id("bia");
    private final UUID caio = id("caio");
    private final UUID duda = id("duda");
    private final List<List<UUID>> parties = new ArrayList<>();
    private MatchService<TestArena, RecordingState> service;

    @BeforeEach
    void setUp() throws IOException {
        ArenaCatalog catalog = new ArenaCatalog(new Arenas(), map -> true, Logger.getAnonymousLogger());
        catalog.loadAll();
        MatchRules rules = MatchRules.defaults("mxt").respawning();
        PartyGroups groups = players -> parties.stream().map(party -> party.stream().filter(players::contains).toList()).filter(party -> party.size() > 1).toList();
        Map<UUID, List<String>> told = new HashMap<>();
        service = new MatchService<>(new Game(rules), List.of(ONE_V_ONE, TWO_V_TWO), catalog, new SingleCells(), match -> stage,
            RuntimeFixtures.environment(clock, rules, groups), (id, text) -> told.computeIfAbsent(id, key -> new ArrayList<>()).add(text),
            Logger.getAnonymousLogger());
    }

    @Test
    void theLeaderReservesASeatForEveryCompanionInTheSameRoom() {
        Optional<Reserved> reserved = service.reserve(ana, List.of(bia, caio), "2v2", Optional.empty(), Optional.empty());

        assertThat(reserved).isPresent();
        assertThat(service.summaries()).singleElement().satisfies(room -> assertThat(room.players()).isEqualTo(3));
        arrive(ana);
        arrive(caio);
        arrive(bia);
        assertThat(service.of(bia)).isEqualTo(service.of(ana)).isEqualTo(service.of(caio));
    }

    @Test
    void aPartyLargerThanTheModeIsRefused() {
        assertThat(service.reserve(ana, List.of(bia, caio), "1v1", Optional.empty(), Optional.empty())).isEmpty();
        assertThat(service.all()).isEmpty();
    }

    @Test
    void aRoomWithoutEnoughFreeSeatsIsSkipped() {
        service.reserve(duda, "2v2", Optional.empty(), Optional.empty());
        arrive(duda);
        String first = service.of(duda).orElseThrow().match().code();
        assertThat(service.reserve(caio, "2v2", Optional.empty(), Optional.empty())).hasValueSatisfying(room -> assertThat(room.code()).isEqualTo(first));

        Optional<Reserved> reserved = service.reserve(ana, List.of(bia, id("eva")), "2v2", Optional.empty(), Optional.empty());

        assertThat(reserved).hasValueSatisfying(room -> {
            assertThat(room.code()).isNotEqualTo(first);
            assertThat(room.created()).isTrue();
        });
    }

    @Test
    void aPartyStaysOnOneTeamAndKeepsItsStats() {
        parties.add(List.of(ana, caio));
        startTwoVTwo(ana, bia, caio, duda);

        Match match = service.of(ana).orElseThrow().match();
        assertThat(team(match, ana)).isEqualTo(team(match, caio));
        assertThat(match.ranked(ana)).isTrue();
        assertThat(stage.messages.get(bia)).doesNotContain(WARNING);
    }

    @Test
    void aPartySplitAcrossTeamsWarnsTheMatchAndLosesItsStats() {
        parties.add(List.of(ana, bia));
        arrive(ana);
        arrive(bia);
        seconds(10);

        Match match = service.of(ana).orElseThrow().match();
        assertThat(match.phase()).isEqualTo(MatchPhase.PLAYING);
        assertThat(team(match, ana)).isNotEqualTo(team(match, bia));
        assertThat(match.ranked(ana)).isFalse();
        assertThat(match.ranked(bia)).isFalse();
        assertThat(stage.messages.get(ana)).contains(WARNING);
        assertThat(stage.messages.get(bia)).contains(WARNING);
    }

    @Test
    void playersOutsideTheSplitPartyKeepTheirStats() {
        parties.add(List.of(ana, bia, caio));
        startTwoVTwo(ana, bia, caio, duda);

        Match match = service.of(ana).orElseThrow().match();
        assertThat(match.ranked(duda)).isTrue();
        assertThat(match.unranked()).containsExactlyInAnyOrder(ana, bia, caio);
        assertThat(stage.messages.get(duda)).contains(WARNING);
    }

    @Test
    void gatheringBeforeTheStartReservesSeatsInTheLeaderRoom() {
        arrive(ana);

        List<UUID> accepted = service.gather(ana, List.of(bia));
        arrive(bia);

        assertThat(accepted).containsExactly(bia);
        assertThat(service.of(bia)).isEqualTo(service.of(ana));
    }

    @Test
    void gatheringIntoAFullWaitingRoomLeavesTheMemberOut() {
        service.reserve(caio, "1v1", Optional.empty(), Optional.empty());
        arrive(caio);
        arrive(ana);

        assertThat(service.gather(ana, List.of(bia))).isEmpty();
    }

    @Test
    void gatheringIntoAStartedMatchMakesTheMembersSpectators() {
        arrive(ana);
        arrive(duda);
        seconds(10);
        MatchRuntime<RecordingState> runtime = service.of(ana).orElseThrow();

        assertThat(service.gather(ana, List.of(bia))).containsExactly(bia);
        arrive(bia);

        assertThat(service.of(bia)).contains(runtime);
        assertThat(runtime.isWatching(bia)).isTrue();
        assertThat(runtime.match().player(bia)).isEmpty();
        assertThat(runtime.isSpectator(bia)).isTrue();
        assertThat(stage.looks.get(bia)).isEqualTo(Stage.Look.SPECTATOR);
        assertThat(runtime.online()).contains(bia);
        assertThat(service.chatView(bia).orElseThrow().audience(true)).contains(ana, duda, bia);
    }

    @Test
    void aSpectatorWhoLeavesIsForgotten() {
        arrive(ana);
        arrive(duda);
        seconds(10);
        service.gather(ana, List.of(bia));
        arrive(bia);

        stage.present.remove(bia);
        service.leave(bia);

        assertThat(service.of(bia)).isEmpty();
        assertThat(service.of(ana).orElseThrow().isWatching(bia)).isFalse();
    }

    @Test
    void anUnusedSpectatorSeatExpires() {
        arrive(ana);
        arrive(duda);
        seconds(10);
        service.gather(ana, List.of(bia));
        clock.advance(Duration.ofSeconds(31));
        seconds(1);

        arrive(bia);

        assertThat(service.of(bia)).isNotEqualTo(service.of(ana));
    }

    @Test
    void membersAlreadyPlayingWithTheLeaderAreNotGatheredAgain() {
        arrive(ana);
        arrive(bia);

        assertThat(service.gather(ana, List.of(bia))).isEmpty();
    }

    private void startTwoVTwo(UUID... players) {
        service.reserve(players[0], List.of(players).subList(1, players.length), "2v2", Optional.empty(), Optional.empty());
        for (UUID player : players) {
            arrive(player);
        }
        seconds(10);
        assertThat(service.of(players[0]).orElseThrow().match().phase()).isEqualTo(MatchPhase.PLAYING);
    }

    private static Object team(Match match, UUID id) {
        return match.player(id).map(Participant::team).orElseThrow();
    }

    private void arrive(UUID id) {
        stage.present.add(id);
        service.arrive(id, id.toString().substring(0, 4));
    }

    private void seconds(int count) {
        for (int second = 0; second < count; second++) {
            service.second();
        }
    }

    private static UUID id(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }

    private record Game(MatchRules rules) implements Minigame<TestArena, RecordingState> {

        @Override
        public String id() {
            return "teste";
        }

        @Override
        public TestArena arena(LoadedArena loaded) {
            return new TestArena(loaded.name(), loaded.displayName(), loaded.mode(), loaded.arena().teams());
        }

        @Override
        public RecordingState open(MatchRuntime<RecordingState> runtime, TestArena arena) {
            return new RecordingState();
        }
    }

    private static final class Arenas implements ArenaStore {

        @Override
        public List<String> names() {
            return List.of("1v1_teste", "2v2_teste");
        }

        @Override
        public Optional<StoredArena> read(String name) {
            ArenaDraft draft = new ArenaDraft(name, name.substring(0, 3), "Teste");
            draft.restoreTeams(TestArena.RED_BLUE);
            return Optional.of(new StoredArena(draft, true));
        }

        @Override
        public Optional<MapTemplate> map(String name) {
            return Optional.of(MapTemplate.empty());
        }

        @Override
        public void save(ArenaDraft draft, boolean ready, MapTemplate map) {
        }

        @Override
        public void saveDefinition(ArenaDraft draft, boolean ready) {
        }

        @Override
        public boolean delete(String name) {
            return false;
        }
    }

    private static final class SingleCells implements Cells {

        private int next;

        @Override
        public Cell allocate(MapTemplate map) {
            return new Cell(next++, 0, 0, 64);
        }

        @Override
        public void release(Cell cell) {
        }

        @Override
        public Optional<Cell> cellAt(Location location) {
            return Optional.empty();
        }

        @Override
        public boolean contains(Location location) {
            return false;
        }
    }
}

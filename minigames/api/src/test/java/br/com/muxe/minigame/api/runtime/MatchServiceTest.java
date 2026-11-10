package br.com.muxe.minigame.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.Participant.State;
import br.com.muxe.minigame.api.match.TestArena;
import br.com.muxe.minigame.api.network.Reserved;
import br.com.muxe.minigame.engine.arena.ArenaCatalog;
import br.com.muxe.minigame.engine.arena.ArenaStore;
import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.ArenaFiles.StoredArena;
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

class MatchServiceTest {

    private static final GameMode ONE_V_ONE = new GameMode("1v1", "1v1", 2, 2, 1);

    private final MutableClock clock = MutableClock.startingAt("2026-11-07T20:00:00Z");
    private final FakeStage stage = new FakeStage();
    private final List<Cell> released = new ArrayList<>();
    private final Map<UUID, List<String>> told = new HashMap<>();
    private final UUID red = UUID.nameUUIDFromBytes("red".getBytes());
    private final UUID blue = UUID.nameUUIDFromBytes("blue".getBytes());
    private MatchService<TestArena, RecordingState> service;

    @BeforeEach
    void setUp() throws IOException {
        ArenaCatalog catalog = new ArenaCatalog(new OneArena(), map -> true, Logger.getAnonymousLogger());
        catalog.loadAll();
        MatchRules rules = MatchRules.defaults("mxt").respawning();
        service = new MatchService<>(new TestGame(rules), List.of(ONE_V_ONE), catalog, new TestCells(released), match -> stage,
            RuntimeFixtures.environment(clock, rules), (id, text) -> told.computeIfAbsent(id, key -> new ArrayList<>()).add(text), Logger.getAnonymousLogger());
    }

    @Test
    void playersArrivingFillTheSameMatch() {
        arrive(red, "red");
        arrive(blue, "blue");

        assertThat(service.all()).hasSize(1);
        assertThat(service.of(red)).isEqualTo(service.of(blue));
        assertThat(service.summaries()).singleElement().satisfies(room -> {
            assertThat(room.players()).isEqualTo(2);
            assertThat(room.open()).isFalse();
        });
    }

    @Test
    void aModeWithoutArenasTellsThePlayer() {
        stage.present.add(red);
        service.join(red, "red", "solo");

        assertThat(service.of(red)).isEmpty();
        assertThat(told.get(red)).containsExactly("sem arena para solo");
    }

    @Test
    void aReservedSeatTakesThePlayerToThatMatchAndCountsBeforeArrival() {
        Optional<Reserved> reserved = service.reserve(red, "1v1", Optional.empty(), Optional.empty());

        assertThat(reserved).hasValueSatisfying(found -> assertThat(found.created()).isTrue());
        assertThat(service.summaries().getFirst().players()).isEqualTo(1);

        arrive(red, "red");

        assertThat(service.of(red).map(runtime -> runtime.match().code())).contains(reserved.get().code());
        assertThat(service.reserve(blue, "duelo", Optional.empty(), Optional.empty())).isEmpty();
    }

    @Test
    void aPlayerWhoLeavesAPlayingMatchReturnsToItWhenArrivingAgain() {
        arrive(red, "red");
        arrive(blue, "blue");
        seconds(10);
        assertThat(service.nameTag(red)).isPresent();
        assertThat(service.activeMatches()).isEqualTo(1);

        stage.present.remove(red);
        service.leave(red);

        assertThat(service.of(red)).isPresent();
        arrive(red, "red");
        assertThat(service.of(red).orElseThrow().match().player(red).orElseThrow().state()).isEqualTo(State.RESPAWNING);
    }

    @Test
    void aRescuedPlayerLeavesTheMatchUnrankedAndAClosedMatchIsNoLongerOpen() {
        arrive(red, "red");
        arrive(blue, "blue");
        seconds(10);
        MatchRuntime<RecordingState> runtime = service.of(red).orElseThrow();
        assertThat(service.inOpenMatch(red)).isTrue();

        stage.present.remove(red);
        service.rescue(red);

        assertThat(runtime.match().ranked(red)).isFalse();
        assertThat(runtime.match().player(red).orElseThrow().state()).isEqualTo(State.DISCONNECTED);
        runtime.close();
        assertThat(service.inOpenMatch(blue)).isFalse();
        assertThat(service.inOpenMatch(UUID.randomUUID())).isFalse();
    }

    @Test
    void chatGoesToTheMatchAndShoutingIsRemembered() {
        arrive(red, "red");
        arrive(blue, "blue");

        assertThat(service.chatView(red).orElseThrow().audience(false)).containsExactlyInAnyOrder(red, blue);
        List<Boolean> during = new ArrayList<>();
        service.shout(red, () -> during.add(service.isShouting(red)));

        assertThat(during).containsExactly(true);
        assertThat(service.isShouting(red)).isFalse();
    }

    @Test
    void staffCanForceTheStartAndTheEnd() {
        arrive(red, "red");

        assertThat(service.forceStart(red)).isTrue();
        seconds(10);
        assertThat(service.of(red).orElseThrow().match().phase()).isEqualTo(MatchPhase.PLAYING);
        assertThat(service.forceEnd(red)).isTrue();
        assertThat(service.of(red).orElseThrow().match().phase()).isEqualTo(MatchPhase.ENDING);
    }

    @Test
    void anEmptyMatchClosesAndFreesItsCell() {
        service.reserve(red, "1v1", Optional.empty(), Optional.empty());
        clock.advance(Duration.ofSeconds(31));

        seconds(61);

        assertThat(service.all()).isEmpty();
        assertThat(released).hasSize(1);
    }

    @Test
    void switchingModesBeforeTheStartLeavesTheWaitingRoom() {
        arrive(red, "red");

        service.switchTo(red, "red", "solo");

        assertThat(service.of(red)).isEmpty();
        assertThat(service.modes()).containsExactly("1v1");
        assertThat(service.modeName("1v1")).isEqualTo("1v1");
    }

    private void arrive(UUID id, String name) {
        stage.present.add(id);
        service.arrive(id, name);
    }

    private void seconds(int count) {
        for (int second = 0; second < count; second++) {
            service.second();
        }
    }

    private record TestGame(MatchRules rules) implements Minigame<TestArena, RecordingState> {

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

    private static final class OneArena implements ArenaStore {

        @Override
        public List<String> names() {
            return List.of("1v1_teste");
        }

        @Override
        public Optional<StoredArena> read(String name) {
            ArenaDraft draft = new ArenaDraft(name, "1v1", "Teste");
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

    private record TestCells(List<Cell> released) implements Cells {

        @Override
        public Cell allocate(MapTemplate map) {
            return new Cell(released.size(), 0, 0, 64);
        }

        @Override
        public void release(Cell cell) {
            released.add(cell);
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

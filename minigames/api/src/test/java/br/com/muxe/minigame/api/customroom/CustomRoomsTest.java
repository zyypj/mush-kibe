package br.com.muxe.minigame.api.customroom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.customroom.CustomRoom.Choice;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.TestArena;
import br.com.muxe.minigame.api.runtime.Cells;
import br.com.muxe.minigame.api.runtime.FakeStage;
import br.com.muxe.minigame.api.runtime.MatchEnvironment;
import br.com.muxe.minigame.api.runtime.MatchEvents;
import br.com.muxe.minigame.api.runtime.MatchRules;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.MatchService;
import br.com.muxe.minigame.api.runtime.Minigame;
import br.com.muxe.minigame.api.runtime.RecordingState;
import br.com.muxe.minigame.api.runtime.RuntimeFixtures;
import br.com.muxe.minigame.api.runtime.TeamPreferences;
import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.engine.arena.ArenaCatalog;
import br.com.muxe.minigame.engine.arena.ArenaStore;
import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.ArenaFiles.StoredArena;
import br.com.muxe.minigame.engine.team.PartyGroups;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.cell.Cell;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomRoomsTest {

    private static final GameMode SOLO = new GameMode("solo", "Solo", 2, 8, 1);
    private static final KnockbackProfile BASE = KnockbackProfile.VANILLA;
    private static final String SCHEMA = """
        root: options
        pages:
          options:
            title: "Opções"
            entries:
              size:
                type: stepper
                slot: 10
                key: team-size
                default: mode-team-size
                min: "1"
                max: mode-players
              count:
                type: stepper
                slot: 11
                key: teams.count
                default: mode-teams
                min: "2"
                max: mode-teams
        """;

    private final MutableClock clock = MutableClock.startingAt("2026-11-09T20:00:00Z");
    private final FakeStage stage = new FakeStage();
    private final FakePort port = new FakePort();
    private final UUID leader = UUID.nameUUIDFromBytes("leader".getBytes());
    private final UUID friend = UUID.nameUUIDFromBytes("friend".getBytes());
    private final UUID other = UUID.nameUUIDFromBytes("other".getBytes());
    private final RoomSchema schema = SchemaReader.parse("teste", SCHEMA);
    private MatchService<TestArena, RecordingState> service;
    private CustomRooms<RecordingState> rooms;

    @BeforeEach
    void setUp() throws IOException {
        ArenaCatalog catalog = new ArenaCatalog(new OneArena(), map -> true, Logger.getAnonymousLogger());
        catalog.loadAll();
        MatchRules rules = MatchRules.defaults("mxt").respawning();
        MatchEnvironment environment = new MatchEnvironment(RuntimeFixtures.settings(), rules, RuntimeFixtures.MESSAGES, clock, new Random(3), MatchEvents.NONE,
            TeamPreferences.NONE, PartyGroups.NONE, mode -> Optional.of(BASE));
        service = new MatchService<>(new TestGame(rules), List.of(SOLO), catalog, new TestCells(), match -> stage, environment, (id, text) -> {
        }, Logger.getAnonymousLogger());
        rooms = new CustomRooms<>(service, port, RuntimeFixtures.MESSAGES, (room, player, standard) -> {
            List<HotbarItem> items = new ArrayList<>(standard);
            items.add(new HotbarItem(3, "NAME_TAG", "time"));
            if (room.isLeader(player)) {
                items.add(new HotbarItem(7, "BREWING_STAND_ITEM", "menu"));
            }
            return items;
        }, " (C)");
    }

    @Test
    void aCreatedRoomIsPrivateLabelledAndCountsThirtySecondsOnceTheSecondPlayerArrives() {
        String code = create();

        assertThat(service.summaries()).singleElement().satisfies(summary -> assertThat(summary.open()).isFalse());
        arrive(leader);
        assertThat(runtime(code).match().phase()).isEqualTo(MatchPhase.WAITING);
        arrive(friend);

        Match match = runtime(code).match();
        assertThat(match.label()).isEqualTo(code + " (C)");
        assertThat(match.phase()).isEqualTo(MatchPhase.STARTING);
        assertThat(match.countdown().remaining()).hasValue(30);
        assertThat(service.of(friend)).contains(runtime(code));
        assertThat(stage.items.get(leader)).extracting(HotbarItem::name).contains("time", "menu");
        assertThat(stage.items.get(friend)).extracting(HotbarItem::name).contains("time").doesNotContain("menu");
    }

    @Test
    void onlyTheLeaderPausesTheCountdownAndStartsTheMatchAtOnce() {
        String code = create();
        arrive(leader);
        arrive(friend);

        assertThat(rooms.pause(friend)).isFalse();
        assertThat(rooms.pause(leader)).isTrue();
        seconds(31);
        assertThat(runtime(code).match().phase()).isEqualTo(MatchPhase.WAITING);
        assertThat(rooms.start(friend)).isFalse();
        assertThat(rooms.start(leader)).isTrue();
        assertThat(runtime(code).match().phase()).isEqualTo(MatchPhase.PLAYING);
    }

    @Test
    void theCountdownStartsTheMatchWhenItEnds() {
        String code = create();
        arrive(leader);
        arrive(friend);

        seconds(30);

        assertThat(runtime(code).match().phase()).isEqualTo(MatchPhase.PLAYING);
    }

    @Test
    void theLeaderCanStartAlone() {
        String code = create();
        arrive(leader);

        rooms.start(leader);

        assertThat(runtime(code).match().phase()).isEqualTo(MatchPhase.PLAYING);
    }

    @Test
    void chosenTeamsAndSpectatorsAreKeptWhenTheMatchStarts() {
        String code = rooms.create(leader, schema, "solo", List.of(), RoomOptions.EMPTY, List.of(friend, other)).orElseThrow();
        arrive(leader);
        arrive(friend);
        arrive(other);

        assertThat(rooms.choose(leader, Optional.of(TeamColor.CINZA))).isEqualTo(Choice.JOINED);
        assertThat(rooms.choose(friend, Optional.of(TeamColor.CINZA))).isEqualTo(Choice.FULL);
        assertThat(rooms.choose(other, Optional.empty())).isEqualTo(Choice.SPECTATOR);
        rooms.start(leader);
        seconds(10);

        Match match = runtime(code).match();
        assertThat(match.player(leader).orElseThrow().team()).isEqualTo(TeamColor.CINZA);
        assertThat(match.player(other)).isEmpty();
        assertThat(runtime(code).isWatching(other)).isTrue();
        assertThat(match.ranked(leader)).isFalse();
    }

    @Test
    void changingTheTeamsReshapesTheWaitingRoomAndRenamesTeams() {
        String code = create();
        arrive(leader);
        CustomRoom room = rooms.room(code).orElseThrow();

        rooms.changeOptions(room, new RoomOptions(Map.of("teams.count", "4", "team-size", "2", "teams.1.name", "Tesão", "knockback.horizontal", "0.75")));

        Match match = runtime(code).match();
        assertThat(match.teams()).hasSize(4);
        assertThat(match.mode().maxPlayers()).isEqualTo(8);
        assertThat(match.teamName(TeamColor.VERMELHO)).isEqualTo("Tesão");
        assertThat(match.teamLetter(TeamColor.VERMELHO)).isEqualTo('T');
        assertThat(runtime(code).knockback().orElseThrow().horizontal()).isEqualTo(0.75);
        assertThat(stage.knockbackOf(leader).orElseThrow().horizontal()).isEqualTo(0.75);
    }

    @Test
    void theRoomGoesBackToItsCountdownAfterTheMatchEnds() {
        String code = create();
        arrive(leader);
        arrive(friend);
        rooms.start(leader);
        seconds(10);
        runtime(code).forceEnd();

        seconds(RuntimeFixtures.settings().timings().restartSeconds() + 1);

        MatchRuntime<RecordingState> restarted = runtime(code);
        assertThat(restarted.match().phase()).isEqualTo(MatchPhase.STARTING);
        assertThat(restarted.match().countdown().remaining().orElseThrow()).isBetween(29, 30);
        assertThat(restarted.match().joinOrder()).containsExactly(leader, friend);
        assertThat(port.lobby).isEmpty();
    }

    @Test
    void theRoomClosesWhenTheLeaderLeaves() {
        String code = create();
        arrive(leader);
        arrive(friend);
        rooms.second();

        stage.present.remove(leader);
        service.leave(leader);
        rooms.second();

        assertThat(port.lobby).contains(friend);
        seconds(4);
        assertThat(service.room(code)).isEmpty();
        assertThat(rooms.room(code)).isEmpty();
    }

    @Test
    void newPartyMembersArePulledAndALostLeadershipClosesTheRoom() {
        String code = create();
        arrive(leader);
        rooms.second();

        port.party = Optional.of(party(leader, true));
        rooms.second();

        assertThat(port.pulled).containsExactly(other);
        port.party = Optional.of(new PartySnapshot(UUID.randomUUID(), friend, false, party(leader, true).members()));
        rooms.second();
        assertThat(rooms.room(code).orElseThrow().closing()).isTrue();
    }

    private PartySnapshot party(UUID leaderId, boolean otherOnline) {
        return new PartySnapshot(UUID.randomUUID(), leaderId, false, List.of(new PartySnapshot.Member(leader, "leader", true),
            new PartySnapshot.Member(friend, "friend", true), new PartySnapshot.Member(other, "other", otherOnline)));
    }

    private String create() {
        return rooms.create(leader, schema, "solo", List.of(), RoomOptions.EMPTY, List.of(friend)).orElseThrow();
    }

    private MatchRuntime<RecordingState> runtime(String code) {
        return service.room(code).orElseThrow();
    }

    private void arrive(UUID id) {
        stage.present.add(id);
        port.names.put(id, id.toString());
        service.arrive(id, id.toString());
    }

    private void seconds(int count) {
        for (int second = 0; second < count; second++) {
            service.second();
            rooms.second();
        }
    }

    private static final class FakePort implements RoomPort {

        private final Map<UUID, String> names = new HashMap<>();
        private final List<UUID> pulled = new ArrayList<>();
        private final List<UUID> lobby = new ArrayList<>();
        private Optional<PartySnapshot> party = Optional.empty();

        @Override
        public Optional<PartySnapshot> party(UUID player) {
            return party;
        }

        @Override
        public Optional<String> onlineName(UUID player) {
            return Optional.ofNullable(names.get(player));
        }

        @Override
        public void pull(UUID leaderId, UUID player) {
            pulled.add(player);
        }

        @Override
        public void toLobby(UUID player) {
            lobby.add(player);
        }

        @Override
        public void message(UUID player, String text) {
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
            return List.of("solo_teste");
        }

        @Override
        public Optional<StoredArena> read(String name) {
            ArenaDraft draft = new ArenaDraft(name, "solo", "Teste");
            draft.restoreTeams(TestArena.EIGHT);
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

    private static final class TestCells implements Cells {

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

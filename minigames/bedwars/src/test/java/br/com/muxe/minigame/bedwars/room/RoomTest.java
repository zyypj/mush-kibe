package br.com.muxe.minigame.bedwars.room;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.cosmetic.animation.Area;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.Participant.State;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.match.TeamStatus.Kind;
import br.com.muxe.minigame.api.view.MatchBoard;
import br.com.muxe.minigame.api.view.NameTagStyle;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class RoomTest {

    private static final Instant NOW = Instant.parse("2026-10-30T20:00:00Z");
    private static final List<TeamColor> RED_BLUE = List.of(TeamColor.VERMELHO, TeamColor.AZUL);
    private static final List<TeamColor> EIGHT = List.of(TeamColor.VERMELHO, TeamColor.AZUL, TeamColor.VERDE, TeamColor.AMARELO,
        TeamColor.CIANO, TeamColor.LARANJA, TeamColor.ROSA, TeamColor.CINZA);

    private final MatchBoard board = new MatchBoard(RoomFixtures.MATCH_SETTINGS.board());

    @Test
    void aRoomWithFewerTeamsThanTheMapOnlyPlaysTheTeamsOfTheMatch() {
        Room room = RoomFixtures.room("2v7few", RoomFixtures.SOLO, RoomFixtures.arena("solo", "Slip", EIGHT));
        room.match().reshape(RoomFixtures.SOLO, RED_BLUE);
        room.match().join(UUID.nameUUIDFromBytes("red".getBytes()), "red");

        assertThat(room.islands()).containsExactlyElementsOf(RED_BLUE);
        assertThat(room.bedAlive(TeamColor.VERDE)).isFalse();
        assertThat(room.emptyIslands()).containsExactlyInAnyOrderElementsOf(EIGHT);
    }

    @Test
    void theMapAreaCoversEveryIslandWithAMarginButNotTheWaitingLobby() {
        Room room = RoomFixtures.room("2v7are", RoomFixtures.ONE_V_ONE, RoomFixtures.arena("1v1", "Slip", RED_BLUE));
        double offset = room.match().cell().offsetBlockX();

        assertThat(room.area()).isEqualTo(new Area(
            offset - 2.5 - Room.MAP_MARGIN, offset + 0.5 - Room.MAP_MARGIN, offset + 43.5 + Room.MAP_MARGIN, offset + 64.5 + Room.MAP_MARGIN));
    }

    @Test
    void theMiddleOfTheMapIsBetweenTheEmeraldGeneratorsNotTheCentreOfTheBox() {
        BedWarsArena arena = RoomFixtures.arena("1v1", "Slip", RED_BLUE, List.of(Point.block(10, 70, 20), Point.block(16, 70, 30)));
        Room room = RoomFixtures.room("2v7are", RoomFixtures.ONE_V_ONE, arena);
        double offset = room.match().cell().offsetBlockX();

        Area area = room.area();

        assertThat(area.middleX()).isEqualTo(offset + 13.5);
        assertThat(area.middleZ()).isEqualTo(offset + 25.5);
        assertThat((area.minX() + area.maxX()) / 2).isNotEqualTo(area.middleX());
    }

    @Test
    void withoutEmeraldGeneratorsTheMiddleIsTheCentreOfTheMap() {
        Room room = RoomFixtures.room("2v7are", RoomFixtures.ONE_V_ONE, RoomFixtures.arena("1v1", "Slip", RED_BLUE));

        Area area = room.area();

        assertThat(area.middleX()).isEqualTo((area.minX() + area.maxX()) / 2);
        assertThat(area.middleZ()).isEqualTo((area.minZ() + area.maxZ()) / 2);
    }

    @Test
    void theWaitingScoreboardMatchesTheReference() {
        Room room = RoomFixtures.room("2v79c1", RoomFixtures.SOLO, RoomFixtures.arena("solo", "Athens", EIGHT));
        UUID first = join(room, "tadeu");
        join(room, "x572");

        assertThat(board.title()).isEqualTo("&b&lBED WARS");
        assertThat(lines(room, first)).containsExactly(
            "&82v79c1", "", "&fMapa: &aAthens", "&fJogadores: &a2/8", "", "&fAguardando...", "", "&fModo: &aSolo", "&fWinstreak: &a0", "", "&amuxe.com.br");
    }

    @Test
    void reachingTheMinimumShowsTheCountdown() {
        Room room = RoomFixtures.room("2v79c1", RoomFixtures.SOLO, RoomFixtures.arena("solo", "Athens", EIGHT));
        UUID first = null;
        for (int index = 0; index < 6; index++) {
            UUID id = join(room, "p" + index);
            first = first == null ? id : first;
        }

        assertThat(lines(room, first)).contains("&fInicia em: &a30s");
    }

    @Test
    void theTwoTeamScoreboardShowsTheNextEventTheTeamsAndTheStats() {
        Room room = RoomFixtures.room("2v7bss", RoomFixtures.ONE_V_ONE, RoomFixtures.arena("1v1", "Slip", RED_BLUE));
        UUID tadeu = join(room, "tadeu");
        join(room, "x572");
        room.match().start(List.of(), NOW);
        for (int second = 0; second < 109; second++) {
            room.match().tick();
        }

        assertThat(lines(room, tadeu)).containsExactly(
            "&82v7bss", "", "&fDiamante II em &a4:11", "",
            "&cV &fVermelho: &a✔ &7VOCÊ", "&9A &fAzul: &a✔", "",
            "&fKills: &a0", "&fKills Finais: &a0", "&fCamas Quebradas: &a0", "", "&amuxe.com.br");
    }

    @Test
    void theTeamLetterIsBoldInTheNameTagAndTabList() {
        assertThat(TeamColor.VERMELHO.boldLetter()).isEqualTo("&lV&c");
        assertThat(NameTagStyle.of(BedWarsGame.NAME_TAG_PREFIX, TeamColor.VERMELHO).prefix()).isEqualTo("§c§lV§c ");
        assertThat(NameTagStyle.of(BedWarsGame.NAME_TAG_PREFIX, TeamColor.VERMELHO).team()).isEqualTo("mxbw04vermelho");
    }

    @Test
    void eightTeamsHideTheStatsAndEmptyTeamsAreOut() {
        Room room = RoomFixtures.room("2v9cid", RoomFixtures.SOLO, RoomFixtures.arena("solo", "Athens", EIGHT));
        UUID first = null;
        for (int index = 0; index < 6; index++) {
            UUID id = join(room, "p" + index);
            first = first == null ? id : first;
        }
        room.match().start(List.of(), NOW);

        List<String> lines = lines(room, first);

        assertThat(lines).hasSize(14).doesNotContain("&fKills: &a0");
        assertThat(lines).contains("&cV &fVermelho: &a✔ &7VOCÊ", "&8C &fCinza: &c✘", "&dR &fRosa: &c✘");
    }

    @Test
    void theIndicatorFollowsBedDisconnectionAndElimination() {
        Room room = RoomFixtures.room("abc123", RoomFixtures.DOUBLES, RoomFixtures.arena("duplas", "Slip", RED_BLUE));
        Match match = room.match();
        List<UUID> ids = List.of(join(room, "a"), join(room, "b"), join(room, "c"), join(room, "d"));
        match.start(List.of(), NOW);
        Participant red1 = match.player(ids.get(0)).orElseThrow();
        Participant red2 = match.player(ids.get(2)).orElseThrow();
        Participant blue = match.player(ids.get(1)).orElseThrow();

        match.disconnected(red1, NOW.plusSeconds(180));
        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.RESPAWNS);
        match.disconnected(red2, NOW.plusSeconds(180));
        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.RESPAWNS_OFFLINE);

        List<Participant> lost = room.breakBed(TeamColor.VERMELHO, blue);

        assertThat(lost).containsExactly(red1, red2);
        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.ELIMINATED);
        assertThat(room.player(blue.id()).beds()).isEqualTo(1);
        assertThat(match.winner()).contains(TeamColor.AZUL);
    }

    @Test
    void dyingWithABedRespawnsWithWeakerToolsAndWithoutOneEliminates() {
        Room room = RoomFixtures.room("abc123", RoomFixtures.ONE_V_ONE, RoomFixtures.arena("1v1", "Slip", RED_BLUE));
        Match match = room.match();
        Participant red = match.player(join(room, "red")).orElseThrow();
        Participant blue = match.player(join(room, "blue")).orElseThrow();
        match.start(List.of(), NOW);
        room.player(red.id()).pickaxeTier(3);

        room.died(red);
        match.die(red, 5);

        assertThat(red.state()).isEqualTo(State.RESPAWNING);
        assertThat(red.respawnAt()).isEqualTo(5);
        assertThat(room.player(red.id()).pickaxeTier()).isEqualTo(2);
        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.RESPAWNS);

        room.breakBed(TeamColor.VERMELHO, blue);
        match.respawned(red, NOW);
        match.die(red, 5);

        assertThat(red.state()).isEqualTo(State.ELIMINATED);
        assertThat(match.killer(red, NOW, true)).contains(blue);
        assertThat(match.winner()).contains(TeamColor.AZUL);
    }

    @Test
    void emptyTeamsLoseTheirBedButKeepTheirIslandWithForgesAndNpcs() {
        Room room = RoomFixtures.room("abc123", RoomFixtures.SOLO, RoomFixtures.arena("solo", "Athens", EIGHT));
        UUID tadeu = join(room, "tadeu");
        room.match().forceStart(10);
        room.match().start(List.of(), NOW);
        TeamColor own = room.match().player(tadeu).orElseThrow().team();

        assertThat(room.islands()).containsExactlyInAnyOrderElementsOf(EIGHT);
        assertThat(EIGHT).filteredOn(color -> color != own).allSatisfy(color -> assertThat(room.bedAlive(color)).isFalse());
        assertThat(room.bedAlive(own)).isTrue();
    }

    @Test
    void theDefaultsMatchTheReference() {
        assertThat(RoomFixtures.MATCH_SETTINGS.timings().respawnProtectionSeconds()).isEqualTo(3);
        assertThat(RoomFixtures.MATCH_SETTINGS.countdownTitles().at(10)).contains("&610 segundos");
        assertThat(RoomFixtures.MATCH_SETTINGS.lobbyGroup()).isEqualTo("bedwars-lobby");
    }

    @Test
    void theStartingToolsAndTheShearsCannotBeDropped() {
        assertThat(BedWarsGame.matchRules().undroppable()).accepts(Material.WOOD_SWORD, Material.WOOD_PICKAXE, Material.DIAMOND_AXE, Material.SHEARS);
        assertThat(BedWarsGame.matchRules().undroppable()).rejects(Material.IRON_SWORD, Material.WOOL);
        assertThat(BedWarsGame.matchRules().respawn()).isTrue();
        assertThat(BedWarsGame.matchRules().breakOnlyPlaced()).isTrue();
    }

    private List<String> lines(Room room, UUID viewer) {
        Match match = room.match();
        Map<String, String> beds = Map.of("beds", String.valueOf(match.player(viewer).map(player -> room.player(player.id()).beds()).orElse(0)));
        return board.lines(match, viewer, RoomFixtures.SETTINGS.timeline().next(match.elapsed()), beds);
    }

    private static UUID join(Room room, String name) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        room.match().join(id, name);
        return id;
    }
}

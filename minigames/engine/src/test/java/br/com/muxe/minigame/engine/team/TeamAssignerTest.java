package br.com.muxe.minigame.engine.team;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamAssignerTest {

    private static final List<TeamColor> FOUR = List.of(TeamColor.AZUL, TeamColor.VERMELHO, TeamColor.VERDE, TeamColor.AMARELO);

    @Test
    void soloPlayersEachGetTheirOwnTeamInJoinOrder() {
        List<UUID> players = players(3);

        Map<TeamColor, List<UUID>> teams = TeamAssigner.assign(FOUR, 1, players, List.of());

        assertThat(teams.get(TeamColor.AZUL)).containsExactly(players.get(0));
        assertThat(teams.get(TeamColor.VERMELHO)).containsExactly(players.get(1));
        assertThat(teams.get(TeamColor.VERDE)).containsExactly(players.get(2));
        assertThat(teams.get(TeamColor.AMARELO)).isEmpty();
    }

    @Test
    void anIncompleteRoomIsSpreadEvenlyAcrossTheTeams() {
        Map<TeamColor, List<UUID>> teams = TeamAssigner.assign(FOUR, 4, players(6), List.of());

        assertThat(teams.values()).extracting(List::size).containsExactly(2, 2, 1, 1);
    }

    @Test
    void aPartyStaysTogetherAndBigPartiesArePlacedFirst() {
        List<UUID> players = players(8);
        List<UUID> pair = List.of(players.get(6), players.get(7));
        List<UUID> trio = List.of(players.get(1), players.get(3), players.get(5));

        Map<TeamColor, List<UUID>> teams = TeamAssigner.assign(FOUR, 4, players, List.of(pair, trio));

        assertThat(teams.get(TeamColor.AZUL)).startsWith(trio.toArray(UUID[]::new));
        assertThat(teams.get(TeamColor.VERMELHO)).startsWith(pair.toArray(UUID[]::new));
        assertThat(teams.values().stream().mapToInt(List::size).sum()).isEqualTo(8);
    }

    @Test
    void aPartyLargerThanATeamSpillsIntoTheNextAndAbsentMembersAreIgnored() {
        List<UUID> players = players(3);
        List<UUID> party = new ArrayList<>(players);
        party.add(UUID.randomUUID());

        Map<TeamColor, List<UUID>> teams = TeamAssigner.assign(List.of(TeamColor.AZUL, TeamColor.VERMELHO), 2, players, List.of(party));

        assertThat(teams.get(TeamColor.AZUL)).containsExactly(players.get(0), players.get(1));
        assertThat(teams.get(TeamColor.VERMELHO)).containsExactly(players.get(2));
    }

    @Test
    void playersBeyondTheRoomSizeAreLeftOut() {
        Map<TeamColor, List<UUID>> teams = TeamAssigner.assign(List.of(TeamColor.AZUL, TeamColor.VERMELHO), 1, players(3), List.of());

        assertThat(teams.values().stream().mapToInt(List::size).sum()).isEqualTo(2);
    }

    private static List<UUID> players(int count) {
        List<UUID> players = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            players.add(new UUID(0, index + 1));
        }
        return players;
    }
}

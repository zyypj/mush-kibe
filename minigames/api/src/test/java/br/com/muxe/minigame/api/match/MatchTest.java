package br.com.muxe.minigame.api.match;

import static br.com.muxe.minigame.api.match.MatchFixtures.DOUBLES;
import static br.com.muxe.minigame.api.match.MatchFixtures.ONE_V_ONE;
import static br.com.muxe.minigame.api.match.MatchFixtures.SOLO;
import static br.com.muxe.minigame.api.match.MatchFixtures.join;
import static br.com.muxe.minigame.api.match.MatchFixtures.match;
import static br.com.muxe.minigame.api.match.TestArena.EIGHT;
import static br.com.muxe.minigame.api.match.TestArena.RED_BLUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.api.match.Participant.State;
import br.com.muxe.minigame.api.match.TeamStatus.Kind;
import br.com.muxe.minigame.engine.match.StartCountdown.Change;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchTest {

    private static final Instant NOW = Instant.parse("2026-10-30T20:00:00Z");

    @Test
    void reachingTheMinimumStartsTheCountdownAndLosingAPlayerGoesBackToWaiting() {
        Match match = match(SOLO, EIGHT, true);
        List<UUID> players = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            players.add(join(match, "p" + index));
        }

        assertThat(match.join(UUID.randomUUID(), "p5")).isEqualTo(Change.STARTED);
        assertThat(match.phase()).isEqualTo(MatchPhase.STARTING);
        assertThat(match.countdown().remaining()).hasValue(30);
        assertThat(match.leaveBeforeStart(players.getFirst())).isEqualTo(Change.CANCELLED);
        assertThat(match.phase()).isEqualTo(MatchPhase.WAITING);
    }

    @Test
    void aManualRoomCountsThirtySecondsFromTheSecondPlayerUntilItIsPaused() {
        Match match = match(SOLO, EIGHT, true);
        match.manual(true);

        assertThat(match.join(UUID.randomUUID(), "leader")).isEqualTo(Change.NONE);
        UUID friend = UUID.randomUUID();
        assertThat(match.join(friend, "friend")).isEqualTo(Change.STARTED);
        assertThat(match.countdown().remaining()).hasValue(30);
        assertThat(match.leaveBeforeStart(friend)).isEqualTo(Change.CANCELLED);
        assertThat(match.phase()).isEqualTo(MatchPhase.WAITING);
        join(match, "friend");
        assertThat(match.pause()).isTrue();

        assertThat(match.join(UUID.randomUUID(), "late")).isEqualTo(Change.NONE);
        assertThat(match.phase()).isEqualTo(MatchPhase.WAITING);
        assertThat(match.countdown().isRunning()).isFalse();
    }

    @Test
    void aFullMatchStopsAcceptingPlayers() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        join(match, "red");
        join(match, "blue");

        assertThat(match.acceptsPlayers()).isFalse();
        assertThatThrownBy(() -> join(match, "late")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dyingWhileTheTeamRespawnsIsADeathAndAfterwardsAFinalDeath() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        Participant red = match.player(join(match, "red")).orElseThrow();
        Participant blue = match.player(join(match, "blue")).orElseThrow();
        match.start(List.of(), NOW);

        match.die(red, 5);

        assertThat(red.state()).isEqualTo(State.RESPAWNING);
        assertThat(red.respawnAt()).isEqualTo(5);
        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.RESPAWNS);

        match.team(TeamColor.VERMELHO).stopRespawning(blue.id());
        match.respawned(red, NOW);
        match.die(red, 5);

        assertThat(red.state()).isEqualTo(State.ELIMINATED);
        assertThat(red.deaths()).isEqualTo(1);
        assertThat(red.finalDeaths()).isEqualTo(1);
        assertThat(match.winner()).contains(TeamColor.AZUL);
    }

    @Test
    void inAGameWithoutRespawnTheFirstDeathEliminates() {
        Match match = match(ONE_V_ONE, RED_BLUE, false);
        Participant red = match.player(join(match, "red")).orElseThrow();
        join(match, "blue");
        match.start(List.of(), NOW);

        assertThat(match.status(TeamColor.VERMELHO)).isEqualTo(new TeamStatus(Kind.LAST_LIFE, 1));

        match.die(red, 5);

        assertThat(red.state()).isEqualTo(State.ELIMINATED);
        assertThat(match.isOver()).isTrue();
        assertThat(match.winner()).contains(TeamColor.AZUL);
    }

    @Test
    void anEliminationIsAFinalDeath() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        Participant red = match.player(join(match, "red")).orElseThrow();
        join(match, "blue");
        match.start(List.of(), NOW);

        match.eliminate(red);

        assertThat(red.finalDeaths()).isEqualTo(1);
        assertThat(red.deaths()).isZero();
        assertThat(red.state()).isEqualTo(State.ELIMINATED);
    }

    @Test
    void theStatusFollowsDisconnectionAndElimination() {
        Match match = match(DOUBLES, RED_BLUE, true);
        List<UUID> ids = List.of(join(match, "a"), join(match, "b"), join(match, "c"), join(match, "d"));
        match.start(List.of(), NOW);
        Participant red1 = match.player(ids.get(0)).orElseThrow();
        Participant red2 = match.player(ids.get(2)).orElseThrow();

        match.disconnected(red1, NOW.plusSeconds(180));
        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.RESPAWNS);
        match.disconnected(red2, NOW.plusSeconds(180));
        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.RESPAWNS_OFFLINE);

        match.team(TeamColor.VERMELHO).stopRespawning(ids.get(1));
        match.eliminate(red1);
        match.eliminate(red2);

        assertThat(match.status(TeamColor.VERMELHO).kind()).isEqualTo(Kind.ELIMINATED);
        assertThat(match.winner()).contains(TeamColor.AZUL);
    }

    @Test
    void aDisconnectedPlayerCanReturnOnlyWhileTheTeamRespawnsAndTheGraceLasts() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        Participant red = match.player(join(match, "red")).orElseThrow();
        join(match, "blue");
        match.start(List.of(), NOW);

        assertThat(match.leavingWhileReturnable(red.id())).isTrue();
        match.disconnected(red, NOW.plusSeconds(180));

        assertThat(match.canReturn(red.id(), NOW.plusSeconds(10))).isTrue();
        assertThat(match.canReturn(red.id(), NOW.plusSeconds(181))).isFalse();

        match.team(TeamColor.VERMELHO).stopRespawning(null);

        assertThat(match.canReturn(red.id(), NOW.plusSeconds(10))).isFalse();
        assertThat(match.leavingWhileReturnable(red.id())).isFalse();
    }

    @Test
    void aFinalKillWithoutAHitterGoesToWhoeverTookTheRespawnAway() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        Participant red = match.player(join(match, "red")).orElseThrow();
        Participant blue = match.player(join(match, "blue")).orElseThrow();
        match.start(List.of(), NOW);
        match.team(TeamColor.VERMELHO).stopRespawning(blue.id());

        assertThat(match.killer(red, NOW, true)).contains(blue);
        assertThat(match.killer(red, NOW, false)).isEmpty();
    }

    @Test
    void aTeammateNeverGetsTheKill() {
        Match match = match(DOUBLES, RED_BLUE, true);
        UUID a = join(match, "a");
        UUID b = join(match, "b");
        UUID c = join(match, "c");
        UUID d = join(match, "d");
        match.start(List.of(List.of(a, b), List.of(c, d)), NOW);
        match.credit().hit(a, b, NOW);

        assertThat(match.killer(match.player(a).orElseThrow(), NOW, false)).isEmpty();
    }

    @Test
    void everyOtherRecentAttackerOfTheVictimGetsAnAssistAndTheKillerTheKill() {
        Match match = match(DOUBLES, RED_BLUE, true);
        UUID a = join(match, "a");
        UUID b = join(match, "b");
        UUID c = join(match, "c");
        UUID d = join(match, "d");
        match.start(List.of(List.of(a, b), List.of(c, d)), NOW);
        Participant victim = match.player(a).orElseThrow();
        Participant killer = match.player(c).orElseThrow();
        match.credit().hit(a, d, NOW);
        match.credit().hit(a, b, NOW);
        match.credit().hit(a, c, NOW.plusSeconds(2));

        match.assists(victim, killer, false, NOW.plusSeconds(3));
        match.credit(killer, true);

        assertThat(match.player(d).orElseThrow().assists()).isEqualTo(1);
        assertThat(match.player(b).orElseThrow().assists()).isZero();
        assertThat(killer.finalKills()).isEqualTo(1);
        assertThat(killer.bestKillStreak()).isEqualTo(1);
    }

    @Test
    void spawnProtectionCoversTheStartAndEndsWhenThePlayerHitsSomeone() {
        Match match = match(SOLO, EIGHT, true);
        UUID tadeu = join(match, "tadeu");
        UUID other = join(match, "x572");
        match.forceStart(10);
        match.start(List.of(), NOW);
        match.protectAll(NOW.plusSeconds(3));
        Participant player = match.player(tadeu).orElseThrow();

        assertThat(player.isProtected(NOW.plusSeconds(2))).isTrue();
        assertThat(player.isProtected(NOW.plusSeconds(3))).isFalse();

        match.struck(tadeu);

        assertThat(player.isProtected(NOW.plusSeconds(1))).isFalse();
        assertThat(match.player(other).orElseThrow().isProtected(NOW.plusSeconds(1))).isTrue();
    }

    @Test
    void aMatchForcedWithASinglePlayerKeepsGoingUntilThatPlayerIsEliminated() {
        Match match = match(SOLO, EIGHT, true);
        Participant player = match.player(join(match, "tadeu")).orElseThrow();
        match.forceStart(10);
        match.start(List.of(), NOW);

        assertThat(match.winner()).contains(player.team());
        assertThat(match.isOver()).isFalse();
        match.die(player, 5);
        assertThat(match.isOver()).isFalse();

        match.team(player.team()).stopRespawning(null);
        match.respawned(player, NOW);
        match.die(player, 5);

        assertThat(match.isOver()).isTrue();
        assertThat(match.winner()).isEmpty();
    }

    @Test
    void emptyTeamsStopRespawningAndAreOutFromTheStart() {
        Match match = match(SOLO, EIGHT, true);
        UUID tadeu = join(match, "tadeu");
        join(match, "x572");
        match.forceStart(10);
        match.start(List.of(), NOW);
        TeamColor own = match.player(tadeu).orElseThrow().team();

        assertThat(EIGHT).filteredOn(color -> !match.team(color).members().isEmpty()).hasSize(2);
        assertThat(match.team(own).respawns()).isTrue();
        assertThat(match.team(TeamColor.CINZA).respawns()).isFalse();
        assertThat(match.status(TeamColor.CINZA).isEliminated()).isTrue();
        assertThat(match.winner()).isEmpty();
    }

    @Test
    void staffCanForceAStartThatPlayersJoiningOrLeavingDoNotCancel() {
        Match match = match(SOLO, EIGHT, true);
        assertThat(match.forceStart(10)).isEmpty();
        UUID first = join(match, "tadeu");

        assertThat(match.forceStart(10)).contains(Change.STARTED);
        UUID second = join(match, "x572");
        match.leaveBeforeStart(second);

        assertThat(match.phase()).isEqualTo(MatchPhase.STARTING);
        assertThat(match.countdown().remaining()).hasValue(10);
        assertThat(match.player(first)).isPresent();
        assertThat(match.forceStart(10)).contains(Change.NONE);
    }

    @Test
    void forcingARunningCountdownShortensIt() {
        Match match = match(SOLO, EIGHT, true);
        for (int index = 0; index < SOLO.minPlayers(); index++) {
            join(match, "player" + index);
        }

        assertThat(match.forceStart(10)).contains(Change.SHORTENED);
        assertThat(match.countdown().remaining()).hasValue(10);
    }

    @Test
    void staffCanOnlyEndAMatchThatIsBeingPlayed() {
        Match match = match(SOLO, EIGHT, true);
        join(match, "tadeu");
        assertThat(match.canForceEnd()).isFalse();

        match.forceStart(10);
        match.start(List.of(), NOW);
        assertThat(match.canForceEnd()).isTrue();

        match.end();
        assertThat(match.canForceEnd()).isFalse();
        assertThat(match.phase()).isEqualTo(MatchPhase.ENDING);
    }

    @Test
    void theCelebrantIsDrawnAmongTheOnlineWinners() {
        Match match = match(DOUBLES, RED_BLUE, true);
        UUID first = join(match, "tadeu");
        UUID second = join(match, "x572");
        match.forceStart(10);
        match.start(List.of(List.of(first, second)), NOW);
        TeamColor team = match.player(first).orElseThrow().team();
        Set<UUID> drawn = new HashSet<>();
        Random random = new Random(7);
        for (int draw = 0; draw < 40; draw++) {
            match.celebrant(team, Set.of(first, second), random).ifPresent(drawn::add);
        }

        assertThat(drawn).containsExactlyInAnyOrder(first, second);
        assertThat(match.celebrant(team, Set.of(), random)).isEmpty();
    }

    @Test
    void onlyBlocksPlacedByPlayersAreRemembered() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);

        match.placed(-1030, 64, 2049);

        assertThat(match.wasPlaced(-1030, 64, 2049)).isTrue();
        assertThat(match.wasPlaced(-1030, 65, 2049)).isFalse();
        match.unplaced(-1030, 64, 2049);
        assertThat(match.wasPlaced(-1030, 64, 2049)).isFalse();
    }

    @Test
    void playersStartOnTheirFavouriteColourWhenTheArenaHasIt() {
        Match match = match(SOLO, EIGHT, true);
        UUID first = join(match, "tadeu");
        UUID second = join(match, "x572");
        UUID third = join(match, "lia");

        match.start(List.of(), Map.of(first, TeamColor.CINZA, third, TeamColor.ROSA), new Random(1), NOW);

        assertThat(match.player(first).orElseThrow().team()).isEqualTo(TeamColor.CINZA);
        assertThat(match.player(third).orElseThrow().team()).isEqualTo(TeamColor.ROSA);
        assertThat(match.player(second).orElseThrow().team()).isEqualTo(TeamColor.VERMELHO);
    }

    @Test
    void anEmptyWaitingMatchIsIdleAfterTheLimitUnlessSomeoneIsComing() {
        Match match = match(SOLO, EIGHT, true);

        assertThat(match.idle(true, 1)).isFalse();
        assertThat(match.idle(false, 2)).isFalse();
        assertThat(match.idle(false, 2)).isTrue();
        join(match, "tadeu");
        assertThat(match.idle(false, 1)).isFalse();
    }

    @Test
    void theClockOnlyRunsWhilePlayingAndAfterTheEnd() {
        Match match = match(ONE_V_ONE, RED_BLUE, true);
        join(match, "red");
        join(match, "blue");
        match.tick();
        assertThat(match.elapsed()).isZero();

        match.start(List.of(), NOW);
        match.tick();
        match.tick();
        match.end();
        match.tick();

        assertThat(match.elapsed()).isEqualTo(2);
        assertThat(match.endingFor()).isEqualTo(1);
        assertThat(match.matchId()).isPresent();
        assertThat(match.startedAt()).isEqualTo(NOW);
    }
}

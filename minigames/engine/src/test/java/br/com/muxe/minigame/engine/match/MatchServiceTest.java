package br.com.muxe.minigame.engine.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.result.Result;
import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchServiceTest {

    private final GameMode duo = new GameMode("duo", "Duo", 2, 3, 1);
    private final MatchService service = new MatchService();

    @Test
    void startsTheCountdownWhenTheMinimumIsReached() {
        Match match = service.create(duo, "arena-1");

        service.join(match.id(), UUID.randomUUID());
        assertThat(match.state()).isEqualTo(MatchState.WAITING);

        service.join(match.id(), UUID.randomUUID());
        assertThat(match.state()).isEqualTo(MatchState.STARTING);
    }

    @Test
    void goesBackToWaitingWhenPlayersLeaveDuringTheCountdown() {
        Match match = service.create(duo, "arena-1");
        UUID first = UUID.randomUUID();
        service.join(match.id(), first);
        service.join(match.id(), UUID.randomUUID());

        service.leave(first);

        assertThat(match.state()).isEqualTo(MatchState.WAITING);
    }

    @Test
    void aPlayerBelongsToOneMatchAtATime() {
        Match first = service.create(duo, "arena-1");
        Match second = service.create(duo, "arena-2");
        UUID player = UUID.randomUUID();
        service.join(first.id(), player);

        Result<Match> result = service.join(second.id(), player);

        assertProblem(result, MatchProblems.ALREADY_IN_MATCH);
        assertThat(service.matchOf(player)).contains(first);
    }

    @Test
    void refusesPlayersWhenFull() {
        Match match = service.create(duo, "arena-1");
        service.join(match.id(), UUID.randomUUID());
        service.join(match.id(), UUID.randomUUID());
        service.join(match.id(), UUID.randomUUID());

        assertProblem(service.join(match.id(), UUID.randomUUID()), MatchProblems.FULL);
    }

    @Test
    void refusesPlayersOnceRunning() {
        Match match = service.create(duo, "arena-1");
        service.join(match.id(), UUID.randomUUID());
        service.join(match.id(), UUID.randomUUID());
        service.start(match.id());

        assertProblem(service.join(match.id(), UUID.randomUUID()), MatchProblems.NOT_JOINABLE);
    }

    @Test
    void closingReleasesEveryPlayer() {
        Match match = service.create(duo, "arena-1");
        UUID player = UUID.randomUUID();
        service.join(match.id(), player);
        service.join(match.id(), UUID.randomUUID());
        service.start(match.id());
        service.end(match.id());

        assertThat(service.close(match.id())).contains(player);
        assertThat(service.matchOf(player)).isEmpty();
        assertThat(service.active()).isEmpty();
    }

    @Test
    void stateChangesOnlyFollowTheLifecycle() {
        Match match = service.create(duo, "arena-1");

        assertThatThrownBy(() -> service.end(match.id())).isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void anyActiveStateCanBeCancelled() {
        for (MatchState state : new MatchState[] {MatchState.WAITING, MatchState.STARTING, MatchState.RUNNING, MatchState.ENDING}) {
            assertThat(state.canTransitionTo(MatchState.CANCELLED)).as(state.name()).isTrue();
        }
        assertThat(MatchState.CLOSED.next()).isEmpty();
    }

    private static void assertProblem(Result<Match> result, String code) {
        assertThat(result).isInstanceOfSatisfying(Result.Failure.class, failure -> assertThat(failure.problem().code()).isEqualTo(code));
    }
}

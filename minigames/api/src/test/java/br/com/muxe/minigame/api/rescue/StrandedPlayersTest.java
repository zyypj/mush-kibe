package br.com.muxe.minigame.api.rescue;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StrandedPlayersTest {

    private final MutableClock clock = MutableClock.startingAt("2026-11-10T20:00:00Z");
    private final StrandedPlayers stranded = new StrandedPlayers();
    private final UUID ana = UUID.nameUUIDFromBytes("ana".getBytes());
    private final UUID bia = UUID.nameUUIDFromBytes("bia".getBytes());

    @Test
    void aPlayerOutsideTheMatchWorldIsRescuedOnlyAfterTheGrace() {
        assertThat(stranded.toRescue(List.of(ana), clock.instant())).isEmpty();

        clock.advance(StrandedPlayers.GRACE.minusSeconds(1));
        assertThat(stranded.toRescue(List.of(ana), clock.instant())).isEmpty();

        clock.advance(Duration.ofSeconds(1));
        assertThat(stranded.toRescue(List.of(ana), clock.instant())).containsExactly(ana);
    }

    @Test
    void aPlayerWhoReachesTheMatchInTimeIsForgotten() {
        stranded.toRescue(List.of(ana), clock.instant());
        clock.advance(Duration.ofSeconds(3));
        stranded.toRescue(List.of(), clock.instant());

        clock.advance(Duration.ofSeconds(3));
        assertThat(stranded.toRescue(List.of(ana), clock.instant())).isEmpty();
    }

    @Test
    void aPlayerStillStrandedIsRescuedAgainOnlyAfterTheRetry() {
        stranded.toRescue(List.of(ana, bia), clock.instant());
        clock.advance(StrandedPlayers.GRACE);
        assertThat(stranded.toRescue(List.of(ana, bia), clock.instant())).containsExactlyInAnyOrder(ana, bia);

        clock.advance(StrandedPlayers.RETRY.minusSeconds(1));
        assertThat(stranded.toRescue(List.of(ana, bia), clock.instant())).isEmpty();

        clock.advance(Duration.ofSeconds(1));
        assertThat(stranded.toRescue(List.of(ana, bia), clock.instant())).containsExactlyInAnyOrder(ana, bia);
    }

    @Test
    void aPlayerRescuedRightAwayWaitsForTheRetryBeforeTheNextAttempt() {
        stranded.rescued(ana, clock.instant());

        clock.advance(StrandedPlayers.GRACE);
        assertThat(stranded.toRescue(List.of(ana), clock.instant())).isEmpty();

        clock.advance(StrandedPlayers.RETRY.minus(StrandedPlayers.GRACE));
        assertThat(stranded.toRescue(List.of(ana), clock.instant())).containsExactly(ana);
    }
}

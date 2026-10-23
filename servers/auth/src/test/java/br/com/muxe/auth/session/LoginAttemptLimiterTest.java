package br.com.muxe.auth.session;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock, 3, Duration.ofMinutes(5));
    private final UUID player = UUID.randomUUID();

    @Test
    void blocksAfterTheMaximumNumberOfFailures() {
        assertThat(limiter.recordFailure(player)).isEqualTo(2);
        assertThat(limiter.recordFailure(player)).isEqualTo(1);
        assertThat(limiter.isBlocked(player)).isFalse();

        assertThat(limiter.recordFailure(player)).isZero();
        assertThat(limiter.isBlocked(player)).isTrue();
    }

    @Test
    void forgetsFailuresOutsideTheWindow() {
        limiter.recordFailure(player);
        limiter.recordFailure(player);
        limiter.recordFailure(player);

        clock.advance(Duration.ofMinutes(5));

        assertThat(limiter.isBlocked(player)).isFalse();
    }

    @Test
    void aSuccessfulLoginResetsTheCount() {
        limiter.recordFailure(player);
        limiter.recordFailure(player);

        limiter.reset(player);

        assertThat(limiter.recordFailure(player)).isEqualTo(2);
    }
}

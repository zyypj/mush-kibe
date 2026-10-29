package br.com.muxe.core.proxy.editor.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final MutableClock clock = MutableClock.startingAt("2026-10-28T12:00:00Z");
    private final RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

    @Test
    void eachAddressHasItsOwnLimitPerWindow() {
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void theLimitResetsWhenTheWindowEnds() {
        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.tryAcquire("a");
        }
        clock.advance(Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("a")).isTrue();
    }

    @Test
    void anAddressIsBlockedOnceItReachesTheLimit() {
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");
        assertThat(limiter.isBlocked("a")).isFalse();

        limiter.tryAcquire("a");

        assertThat(limiter.isBlocked("a")).isTrue();
        assertThat(limiter.isBlocked("b")).isFalse();
    }
}

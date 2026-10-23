package br.com.muxe.core.common.messaging.idempotency.memory;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyStoreTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");

    @Test
    void claimsOncePerWindow() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(clock, 10);

        assertThat(store.tryClaim("a", Duration.ofSeconds(10))).isTrue();
        assertThat(store.tryClaim("a", Duration.ofSeconds(10))).isFalse();

        clock.advance(Duration.ofSeconds(10));

        assertThat(store.tryClaim("a", Duration.ofSeconds(10))).isTrue();
    }

    @Test
    void neverGrowsBeyondItsLimit() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(clock, 3);

        for (int index = 0; index < 10; index++) {
            store.tryClaim("key-" + index, Duration.ofMinutes(1));
        }

        assertThat(store.size()).isEqualTo(3);
        assertThat(store.tryClaim("key-9", Duration.ofMinutes(1))).isFalse();
    }
}

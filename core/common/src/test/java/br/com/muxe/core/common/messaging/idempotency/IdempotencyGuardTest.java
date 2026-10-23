package br.com.muxe.core.common.messaging.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.messaging.idempotency.memory.InMemoryIdempotencyStore;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class IdempotencyGuardTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final InMemoryIdempotencyStore distributed = new InMemoryIdempotencyStore(clock, 1_000);
    private final IdempotencyGuard guard = guardFor("lobby-1", distributed);

    @Test
    void volatilePacketsAreNeverDeduplicated() {
        DeliveryPolicy policy = DeliveryPolicy.volatileState(Duration.ofSeconds(5));

        assertThat(guard.claim("server.status", "same", policy)).isEqualTo(ClaimResult.FIRST);
        assertThat(guard.claim("server.status", "same", policy)).isEqualTo(ClaimResult.FIRST);
    }

    @Test
    void localPacketsAreDeduplicatedWithinTheWindow() {
        DeliveryPolicy policy = DeliveryPolicy.standard();

        assertThat(guard.claim("player.connect", "key", policy)).isEqualTo(ClaimResult.FIRST);
        assertThat(guard.claim("player.connect", "key", policy)).isEqualTo(ClaimResult.DUPLICATE);

        clock.advance(policy.deduplicationWindow().plusSeconds(1));

        assertThat(guard.claim("player.connect", "key", policy)).isEqualTo(ClaimResult.FIRST);
    }

    @Test
    void durablePacketsSurviveARestartOfTheReceiver() {
        DeliveryPolicy policy = DeliveryPolicy.durable(Duration.ofMinutes(5));
        guard.claim("auth.authenticated", "key", policy);

        IdempotencyGuard restarted = guardFor("lobby-1", distributed);

        assertThat(restarted.claim("auth.authenticated", "key", policy)).isEqualTo(ClaimResult.DUPLICATE);
    }

    @Test
    void durableClaimsAreScopedPerReceiver() {
        DeliveryPolicy policy = DeliveryPolicy.durable(Duration.ofMinutes(5));
        guard.claim("auth.authenticated", "key", policy);

        assertThat(guardFor("lobby-2", distributed).claim("auth.authenticated", "key", policy)).isEqualTo(ClaimResult.FIRST);
    }

    @Test
    void exclusiveClaimsAreSharedByTheWholeNetwork() {
        DeliveryPolicy policy = DeliveryPolicy.exclusive(Duration.ofMinutes(5));
        guard.claim("store.delivery", "purchase-1", policy);

        assertThat(guardFor("lobby-2", distributed).claim("store.delivery", "purchase-1", policy)).isEqualTo(ClaimResult.DUPLICATE);
    }

    @Test
    void exclusiveClaimsFailClosedWhenTheStoreIsDown() {
        IdempotencyGuard offline = guardFor("lobby-1", (key, ttl) -> {
            throw new IllegalStateException("redis down");
        });

        assertThat(offline.claim("store.delivery", "purchase-1", DeliveryPolicy.exclusive(Duration.ofMinutes(5)))).isEqualTo(ClaimResult.UNAVAILABLE);
    }

    @Test
    void durableClaimsFallBackToMemoryWhenTheStoreIsDown() {
        IdempotencyGuard offline = guardFor("lobby-1", (key, ttl) -> {
            throw new IllegalStateException("redis down");
        });
        DeliveryPolicy policy = DeliveryPolicy.durable(Duration.ofMinutes(5));

        assertThat(offline.claim("auth.authenticated", "key", policy)).isEqualTo(ClaimResult.FIRST);
        assertThat(offline.claim("auth.authenticated", "key", policy)).isEqualTo(ClaimResult.DUPLICATE);
    }

    private IdempotencyGuard guardFor(String receiver, IdempotencyStore store) {
        return new IdempotencyGuard(receiver, new InMemoryIdempotencyStore(clock, 1_000), store, Logger.getLogger("test"));
    }
}

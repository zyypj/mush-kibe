package br.com.muxe.core.proxy.premium;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.store.ExpiringStore;
import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CachedPremiumLookupTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final InMemoryExpiringStore store = new InMemoryExpiringStore(clock);
    private final SimpleMetricRegistry metrics = new SimpleMetricRegistry();
    private final AtomicInteger calls = new AtomicInteger();

    @Test
    void answersRepeatedLookupsFromTheCacheIgnoringCase() {
        CachedPremiumLookup lookup = lookup(PremiumStatus.PREMIUM, store);

        lookup.lookup("Notch");
        PremiumStatus second = lookup.lookup("NOTCH");

        assertThat(second).isEqualTo(PremiumStatus.PREMIUM);
        assertThat(calls).hasValue(1);
        assertThat(metrics.snapshot().counterTotal("muxe_auth_premium_lookups_total")).isEqualTo(2);
    }

    @Test
    void asksAgainOnceTheCacheExpires() {
        CachedPremiumLookup lookup = lookup(PremiumStatus.CRACKED, store);
        lookup.lookup("Steve");

        clock.advance(Duration.ofHours(7));
        lookup.lookup("Steve");

        assertThat(calls).hasValue(2);
    }

    @Test
    void forgetsCrackedAnswersSoonerThanPremiumOnes() {
        CachedPremiumLookup cracked = lookup(PremiumStatus.CRACKED, store);
        cracked.lookup("Steve");
        CachedPremiumLookup premium = lookup(PremiumStatus.PREMIUM, store);
        premium.lookup("Notch");

        clock.advance(Duration.ofMinutes(11));
        cracked.lookup("Steve");
        premium.lookup("Notch");

        assertThat(calls).hasValue(3);
    }

    @Test
    void neverCachesUnknownAnswers() {
        CachedPremiumLookup lookup = lookup(PremiumStatus.UNKNOWN, store);

        lookup.lookup("Steve");
        lookup.lookup("Steve");

        assertThat(calls).hasValue(2);
    }

    @Test
    void keepsWorkingWhenTheCacheIsDown() {
        ExpiringStore broken = new ExpiringStore() {
            @Override
            public Optional<String> get(String key) {
                throw new IllegalStateException("redis down");
            }

            @Override
            public void put(String key, String value, Duration ttl) {
                throw new IllegalStateException("redis down");
            }

            @Override
            public boolean delete(String key) {
                throw new IllegalStateException("redis down");
            }
        };

        assertThat(lookup(PremiumStatus.PREMIUM, broken).lookup("Notch")).isEqualTo(PremiumStatus.PREMIUM);
    }

    private CachedPremiumLookup lookup(PremiumStatus answer, ExpiringStore cache) {
        PremiumLookup remote = username -> {
            calls.incrementAndGet();
            return answer;
        };
        return new CachedPremiumLookup(remote, cache, Duration.ofHours(6), Duration.ofMinutes(10), metrics, Logger.getLogger("test"));
    }
}

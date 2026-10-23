package br.com.muxe.core.common.messaging.idempotency.memory;

import br.com.muxe.core.common.messaging.idempotency.IdempotencyStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Clock clock;
    private final int maxEntries;
    private final LinkedHashMap<String, Instant> claims = new LinkedHashMap<>();

    public InMemoryIdempotencyStore(Clock clock, int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.clock = clock;
        this.maxEntries = maxEntries;
    }

    @Override
    public synchronized boolean tryClaim(String key, Duration ttl) {
        Instant now = clock.instant();
        Instant expiresAt = claims.get(key);
        if (expiresAt != null && expiresAt.isAfter(now)) {
            return false;
        }
        claims.remove(key);
        claims.put(key, now.plus(ttl));
        evict(now);
        return true;
    }

    public synchronized int size() {
        return claims.size();
    }

    private void evict(Instant now) {
        if (claims.size() <= maxEntries) {
            return;
        }
        claims.values().removeIf(expiresAt -> !expiresAt.isAfter(now));
        Iterator<Map.Entry<String, Instant>> oldest = claims.entrySet().iterator();
        while (claims.size() > maxEntries && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }
}

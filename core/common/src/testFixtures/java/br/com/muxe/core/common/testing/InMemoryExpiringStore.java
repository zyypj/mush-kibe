package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.store.ExpiringStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryExpiringStore implements ExpiringStore {

    private final Clock clock;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public InMemoryExpiringStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<String> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null || !entry.expiresAt().isAfter(clock.instant())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        entries.put(key, new Entry(value, clock.instant().plus(ttl)));
    }

    @Override
    public boolean delete(String key) {
        return entries.remove(key) != null;
    }

    private record Entry(String value, Instant expiresAt) {
    }
}

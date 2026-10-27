package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.parkour.domain.ParkourRecordRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryParkourRecordRepository implements ParkourRecordRepository {

    private final Map<String, Duration> records = new ConcurrentHashMap<>();

    @Override
    public Optional<Duration> best(String group, UUID playerId) {
        return Optional.ofNullable(records.get(key(group, playerId)));
    }

    @Override
    public void save(String group, UUID playerId, Duration best, Instant achievedAt) {
        records.merge(key(group, playerId), best, (current, next) -> next.compareTo(current) < 0 ? next : current);
    }

    private static String key(String group, UUID playerId) {
        return group + "/" + playerId;
    }
}

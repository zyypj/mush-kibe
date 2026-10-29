package br.com.muxe.core.common.parkour.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ParkourRecordRepository {

    Optional<Duration> best(String group, UUID playerId);

    void save(String group, UUID playerId, Duration best, Instant achievedAt);
}

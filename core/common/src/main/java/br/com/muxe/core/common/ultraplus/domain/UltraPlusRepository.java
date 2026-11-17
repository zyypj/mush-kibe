package br.com.muxe.core.common.ultraplus.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UltraPlusRepository {

    Optional<UltraPlusProfile> find(UUID playerId);

    void save(UltraPlusProfile profile, Instant updatedAt);
}

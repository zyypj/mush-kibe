package br.com.muxe.core.common.visibility.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VisibilityRepository {

    Optional<Visibility> find(UUID playerId);

    void save(UUID playerId, Visibility visibility, Instant updatedAt);
}

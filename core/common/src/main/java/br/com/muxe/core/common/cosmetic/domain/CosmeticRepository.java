package br.com.muxe.core.common.cosmetic.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CosmeticRepository {

    Set<String> owned(UUID playerId, String category);

    boolean own(UUID playerId, String category, String cosmeticId, String source, Instant at);

    Optional<String> selected(UUID playerId, String category);

    void select(UUID playerId, String category, String cosmeticId, Instant at);
}

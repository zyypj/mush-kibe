package br.com.muxe.core.common.language.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LanguageRepository {

    Optional<Language> find(UUID playerId);

    void save(UUID playerId, Language language, Instant updatedAt);
}

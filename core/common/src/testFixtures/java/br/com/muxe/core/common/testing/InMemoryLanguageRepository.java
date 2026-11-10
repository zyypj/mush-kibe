package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryLanguageRepository implements LanguageRepository {

    private final Map<UUID, Language> languages = new HashMap<>();
    private final Map<UUID, Instant> updates = new HashMap<>();

    @Override
    public Optional<Language> find(UUID playerId) {
        return Optional.ofNullable(languages.get(playerId));
    }

    @Override
    public void save(UUID playerId, Language language, Instant updatedAt) {
        languages.put(playerId, language);
        updates.put(playerId, updatedAt);
    }

    public Optional<Instant> updatedAt(UUID playerId) {
        return Optional.ofNullable(updates.get(playerId));
    }
}

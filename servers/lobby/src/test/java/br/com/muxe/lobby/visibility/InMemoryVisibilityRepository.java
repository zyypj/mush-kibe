package br.com.muxe.lobby.visibility;

import br.com.muxe.lobby.visibility.domain.Visibility;
import br.com.muxe.lobby.visibility.domain.VisibilityRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryVisibilityRepository implements VisibilityRepository {

    private final Map<UUID, Visibility> settings = new HashMap<>();
    private final Map<UUID, Instant> updates = new HashMap<>();

    @Override
    public Optional<Visibility> find(UUID playerId) {
        return Optional.ofNullable(settings.get(playerId));
    }

    @Override
    public void save(UUID playerId, Visibility visibility, Instant updatedAt) {
        settings.put(playerId, visibility);
        updates.put(playerId, updatedAt);
    }

    public Optional<Instant> updatedAt(UUID playerId) {
        return Optional.ofNullable(updates.get(playerId));
    }
}

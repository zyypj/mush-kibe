package br.com.muxe.core.common.ultraplus.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryUltraPlusRepository implements UltraPlusRepository {

    private final Map<UUID, UltraPlusProfile> profiles = new HashMap<>();

    @Override
    public Optional<UltraPlusProfile> find(UUID playerId) {
        return Optional.ofNullable(profiles.get(playerId));
    }

    @Override
    public void save(UltraPlusProfile profile, Instant updatedAt) {
        profiles.put(profile.playerId(), profile);
    }
}

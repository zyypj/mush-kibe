package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.cosmetic.domain.CosmeticRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InMemoryCosmeticRepository implements CosmeticRepository {

    private final Map<String, Set<String>> owned = new HashMap<>();
    private final Map<String, String> selected = new HashMap<>();

    @Override
    public Set<String> owned(UUID playerId, String category) {
        return Set.copyOf(owned.getOrDefault(key(playerId, category), Set.of()));
    }

    @Override
    public boolean own(UUID playerId, String category, String cosmeticId, String source, Instant at) {
        return owned.computeIfAbsent(key(playerId, category), ignored -> new LinkedHashSet<>()).add(cosmeticId);
    }

    @Override
    public Optional<String> selected(UUID playerId, String category) {
        return Optional.ofNullable(selected.get(key(playerId, category)));
    }

    @Override
    public void select(UUID playerId, String category, String cosmeticId, Instant at) {
        selected.put(key(playerId, category), cosmeticId);
    }

    private static String key(UUID playerId, String category) {
        return playerId + "/" + category;
    }
}

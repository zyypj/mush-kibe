package br.com.muxe.core.common.cosmetic.domain;

import java.util.Optional;
import java.util.Set;

public record CosmeticProfile(Set<String> owned, Optional<String> selected) {

    public static final CosmeticProfile EMPTY = new CosmeticProfile(Set.of(), Optional.empty());

    public CosmeticProfile {
        owned = Set.copyOf(owned);
    }

    public boolean owns(String cosmeticId) {
        return owned.contains(cosmeticId);
    }
}

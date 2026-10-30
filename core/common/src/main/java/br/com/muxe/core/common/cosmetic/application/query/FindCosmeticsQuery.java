package br.com.muxe.core.common.cosmetic.application.query;

import br.com.muxe.core.common.cosmetic.domain.CosmeticProfile;
import br.com.muxe.core.common.cqrs.query.Query;
import java.util.Objects;
import java.util.UUID;

public record FindCosmeticsQuery(UUID playerId, String category) implements Query<CosmeticProfile> {

    public FindCosmeticsQuery {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(category, "category");
    }
}

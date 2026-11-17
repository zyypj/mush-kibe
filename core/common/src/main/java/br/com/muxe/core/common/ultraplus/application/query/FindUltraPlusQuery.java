package br.com.muxe.core.common.ultraplus.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusProfile;
import java.util.Objects;
import java.util.UUID;

public record FindUltraPlusQuery(UUID playerId) implements Query<UltraPlusProfile> {

    public FindUltraPlusQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

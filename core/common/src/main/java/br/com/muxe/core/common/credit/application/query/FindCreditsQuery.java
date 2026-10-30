package br.com.muxe.core.common.credit.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import java.util.Objects;
import java.util.UUID;

public record FindCreditsQuery(UUID playerId) implements Query<Long> {

    public FindCreditsQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

package br.com.muxe.core.common.visibility.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.util.Objects;
import java.util.UUID;

public record FindVisibilityQuery(UUID playerId) implements Query<Visibility> {

    public FindVisibilityQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

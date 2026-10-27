package br.com.muxe.core.common.placement.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.placement.domain.Placements;
import java.util.Objects;

public record FindPlacementsQuery(String group) implements Query<Placements> {

    public FindPlacementsQuery {
        Objects.requireNonNull(group, "group");
    }
}

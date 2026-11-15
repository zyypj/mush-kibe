package br.com.muxe.core.common.stats.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.stats.domain.BedWarsTotals;
import java.util.Objects;
import java.util.UUID;

public record FindBedWarsTotalsQuery(UUID playerId) implements Query<BedWarsTotals> {

    public FindBedWarsTotalsQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

package br.com.muxe.core.common.stats;

import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.stats.application.query.FindBedWarsTotalsHandler;
import br.com.muxe.core.common.stats.application.query.FindBedWarsTotalsQuery;
import br.com.muxe.core.common.stats.domain.BedWarsTotalsRepository;

public final class StatsModule {

    private StatsModule() {
    }

    public static void register(QueryBus queries, BedWarsTotalsRepository totals, Transactions transactions) {
        queries.register(FindBedWarsTotalsQuery.class, new FindBedWarsTotalsHandler(totals, transactions));
    }
}

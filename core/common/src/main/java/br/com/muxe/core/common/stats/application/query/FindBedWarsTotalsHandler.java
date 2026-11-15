package br.com.muxe.core.common.stats.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.stats.domain.BedWarsTotals;
import br.com.muxe.core.common.stats.domain.BedWarsTotalsRepository;

public final class FindBedWarsTotalsHandler implements QueryHandler<FindBedWarsTotalsQuery, BedWarsTotals> {

    private final BedWarsTotalsRepository totals;
    private final Transactions transactions;

    public FindBedWarsTotalsHandler(BedWarsTotalsRepository totals, Transactions transactions) {
        this.totals = totals;
        this.transactions = transactions;
    }

    @Override
    public BedWarsTotals handle(FindBedWarsTotalsQuery query) {
        return transactions.readOnly("stats.bedwars.totals", () -> totals.totals(query.playerId()));
    }
}

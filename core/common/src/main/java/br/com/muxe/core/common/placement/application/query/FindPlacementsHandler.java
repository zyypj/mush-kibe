package br.com.muxe.core.common.placement.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.placement.domain.PlacementRepository;
import br.com.muxe.core.common.placement.domain.Placements;

public final class FindPlacementsHandler implements QueryHandler<FindPlacementsQuery, Placements> {

    private final PlacementRepository placements;
    private final Transactions transactions;

    public FindPlacementsHandler(PlacementRepository placements, Transactions transactions) {
        this.placements = placements;
        this.transactions = transactions;
    }

    @Override
    public Placements handle(FindPlacementsQuery query) {
        return transactions.readOnly("placement.find", () -> placements.findByGroup(query.group()));
    }
}

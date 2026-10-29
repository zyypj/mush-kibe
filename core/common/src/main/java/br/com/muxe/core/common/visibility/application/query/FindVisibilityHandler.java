package br.com.muxe.core.common.visibility.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.visibility.domain.Visibility;
import br.com.muxe.core.common.visibility.domain.VisibilityRepository;

public final class FindVisibilityHandler implements QueryHandler<FindVisibilityQuery, Visibility> {

    private final VisibilityRepository settings;
    private final Transactions transactions;

    public FindVisibilityHandler(VisibilityRepository settings, Transactions transactions) {
        this.settings = settings;
        this.transactions = transactions;
    }

    @Override
    public Visibility handle(FindVisibilityQuery query) {
        return transactions.readOnly("visibility.find", () -> settings.find(query.playerId())).orElse(Visibility.ALL);
    }
}

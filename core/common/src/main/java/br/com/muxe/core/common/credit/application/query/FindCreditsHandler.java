package br.com.muxe.core.common.credit.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;

public final class FindCreditsHandler implements QueryHandler<FindCreditsQuery, Long> {

    private final CreditRepository credits;
    private final Transactions transactions;

    public FindCreditsHandler(CreditRepository credits, Transactions transactions) {
        this.credits = credits;
        this.transactions = transactions;
    }

    @Override
    public Long handle(FindCreditsQuery query) {
        return transactions.readOnly("credits.find", () -> credits.balance(query.playerId()));
    }
}

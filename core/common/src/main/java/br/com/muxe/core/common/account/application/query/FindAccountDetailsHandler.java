package br.com.muxe.core.common.account.application.query;

import br.com.muxe.core.common.account.domain.AccountDetails;
import br.com.muxe.core.common.account.domain.AccountDetailsRepository;
import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.Optional;

public final class FindAccountDetailsHandler implements QueryHandler<FindAccountDetailsQuery, Optional<AccountDetails>> {

    private final AccountDetailsRepository details;
    private final Transactions transactions;

    public FindAccountDetailsHandler(AccountDetailsRepository details, Transactions transactions) {
        this.details = details;
        this.transactions = transactions;
    }

    @Override
    public Optional<AccountDetails> handle(FindAccountDetailsQuery query) {
        return transactions.readOnly("account.find_details", () -> query.id().isPresent()
            ? details.findById(query.id().get())
            : details.findByUsername(query.username().orElseThrow()));
    }
}

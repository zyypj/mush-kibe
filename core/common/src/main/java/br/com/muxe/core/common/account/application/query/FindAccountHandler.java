package br.com.muxe.core.common.account.application.query;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountRepository;
import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.Optional;

public final class FindAccountHandler implements QueryHandler<FindAccountQuery, Optional<Account>> {

    private final AccountRepository accounts;
    private final Transactions transactions;

    public FindAccountHandler(AccountRepository accounts, Transactions transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Override
    public Optional<Account> handle(FindAccountQuery query) {
        return transactions.readOnly("account.find", () -> accounts.findById(query.playerId()));
    }
}

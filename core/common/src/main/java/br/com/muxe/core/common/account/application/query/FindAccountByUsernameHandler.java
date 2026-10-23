package br.com.muxe.core.common.account.application.query;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountRepository;
import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.Optional;

public final class FindAccountByUsernameHandler implements QueryHandler<FindAccountByUsernameQuery, Optional<Account>> {

    private final AccountRepository accounts;
    private final Transactions transactions;

    public FindAccountByUsernameHandler(AccountRepository accounts, Transactions transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Override
    public Optional<Account> handle(FindAccountByUsernameQuery query) {
        return transactions.readOnly("account.find_by_username", () -> accounts.findByUsername(query.username()));
    }
}

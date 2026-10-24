package br.com.muxe.core.common.account;

import br.com.muxe.core.common.account.application.command.RecordLoginCommand;
import br.com.muxe.core.common.account.application.command.RecordLoginHandler;
import br.com.muxe.core.common.account.application.query.FindAccountByUsernameHandler;
import br.com.muxe.core.common.account.application.query.FindAccountByUsernameQuery;
import br.com.muxe.core.common.account.application.query.FindAccountHandler;
import br.com.muxe.core.common.account.application.query.FindAccountQuery;
import br.com.muxe.core.common.account.domain.AccountRepository;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class AccountModule {

    private AccountModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, AccountRepository accounts, Transactions transactions, Clock clock) {
        commands.register(RecordLoginCommand.class, new RecordLoginHandler(accounts, transactions, clock));
        queries.register(FindAccountQuery.class, new FindAccountHandler(accounts, transactions));
        queries.register(FindAccountByUsernameQuery.class, new FindAccountByUsernameHandler(accounts, transactions));
    }
}

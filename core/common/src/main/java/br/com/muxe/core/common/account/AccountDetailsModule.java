package br.com.muxe.core.common.account;

import br.com.muxe.core.common.account.application.command.SaveAccountLocationCommand;
import br.com.muxe.core.common.account.application.command.SaveAccountLocationHandler;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsHandler;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsQuery;
import br.com.muxe.core.common.account.domain.AccountDetailsRepository;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class AccountDetailsModule {

    private AccountDetailsModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, AccountDetailsRepository details, Transactions transactions, Clock clock) {
        commands.register(SaveAccountLocationCommand.class, new SaveAccountLocationHandler(details, transactions, clock));
        queries.register(FindAccountDetailsQuery.class, new FindAccountDetailsHandler(details, transactions));
    }
}

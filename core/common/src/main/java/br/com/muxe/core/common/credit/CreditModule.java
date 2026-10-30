package br.com.muxe.core.common.credit;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.credit.application.command.ChangeCreditsCommand;
import br.com.muxe.core.common.credit.application.command.ChangeCreditsHandler;
import br.com.muxe.core.common.credit.application.query.FindCreditsHandler;
import br.com.muxe.core.common.credit.application.query.FindCreditsQuery;
import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class CreditModule {

    private CreditModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, CreditRepository credits, Transactions transactions, Clock clock) {
        commands.register(ChangeCreditsCommand.class, new ChangeCreditsHandler(credits, transactions, clock));
        queries.register(FindCreditsQuery.class, new FindCreditsHandler(credits, transactions));
    }
}

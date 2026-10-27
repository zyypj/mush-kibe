package br.com.muxe.core.common.visibility;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.visibility.application.command.ChangeVisibilityCommand;
import br.com.muxe.core.common.visibility.application.command.ChangeVisibilityHandler;
import br.com.muxe.core.common.visibility.application.query.FindVisibilityHandler;
import br.com.muxe.core.common.visibility.application.query.FindVisibilityQuery;
import br.com.muxe.core.common.visibility.domain.VisibilityRepository;
import java.time.Clock;

public final class VisibilityModule {

    private VisibilityModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, VisibilityRepository settings, Transactions transactions, Clock clock) {
        commands.register(ChangeVisibilityCommand.class, new ChangeVisibilityHandler(settings, transactions, clock));
        queries.register(FindVisibilityQuery.class, new FindVisibilityHandler(settings, transactions));
    }
}

package br.com.muxe.core.common.language;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.language.application.command.ChangeLanguageCommand;
import br.com.muxe.core.common.language.application.command.ChangeLanguageHandler;
import br.com.muxe.core.common.language.application.query.FindLanguageHandler;
import br.com.muxe.core.common.language.application.query.FindLanguageQuery;
import br.com.muxe.core.common.language.domain.LanguageRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class LanguageModule {

    private LanguageModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, LanguageRepository languages, Transactions transactions, Clock clock) {
        commands.register(ChangeLanguageCommand.class, new ChangeLanguageHandler(languages, transactions, clock));
        queries.register(FindLanguageQuery.class, new FindLanguageHandler(languages, transactions));
    }
}

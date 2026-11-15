package br.com.muxe.core.common.preference;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.preference.application.command.ChangePreferenceCommand;
import br.com.muxe.core.common.preference.application.command.ChangePreferenceHandler;
import br.com.muxe.core.common.preference.application.query.FindPreferencesHandler;
import br.com.muxe.core.common.preference.application.query.FindPreferencesQuery;
import br.com.muxe.core.common.preference.domain.PreferenceRepository;
import java.time.Clock;

public final class PreferenceModule {

    private PreferenceModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, PreferenceRepository preferences, Transactions transactions, Clock clock) {
        commands.register(ChangePreferenceCommand.class, new ChangePreferenceHandler(preferences, transactions, clock));
        queries.register(FindPreferencesQuery.class, new FindPreferencesHandler(preferences, transactions));
    }
}

package br.com.muxe.core.common.preference.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.preference.domain.PreferenceRepository;
import br.com.muxe.core.common.preference.domain.Preferences;

public final class FindPreferencesHandler implements QueryHandler<FindPreferencesQuery, Preferences> {

    private final PreferenceRepository preferences;
    private final Transactions transactions;

    public FindPreferencesHandler(PreferenceRepository preferences, Transactions transactions) {
        this.preferences = preferences;
        this.transactions = transactions;
    }

    @Override
    public Preferences handle(FindPreferencesQuery query) {
        return transactions.readOnly("preference.find", () -> preferences.find(query.playerId()));
    }
}

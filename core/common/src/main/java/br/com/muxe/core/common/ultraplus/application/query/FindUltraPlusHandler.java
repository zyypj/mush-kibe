package br.com.muxe.core.common.ultraplus.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusProfile;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusRepository;

public final class FindUltraPlusHandler implements QueryHandler<FindUltraPlusQuery, UltraPlusProfile> {

    private final UltraPlusRepository profiles;
    private final Transactions transactions;

    public FindUltraPlusHandler(UltraPlusRepository profiles, Transactions transactions) {
        this.profiles = profiles;
        this.transactions = transactions;
    }

    @Override
    public UltraPlusProfile handle(FindUltraPlusQuery query) {
        return transactions.readOnly("ultraplus.find", () -> profiles.find(query.playerId()).orElseGet(() -> UltraPlusProfile.empty(query.playerId())));
    }
}

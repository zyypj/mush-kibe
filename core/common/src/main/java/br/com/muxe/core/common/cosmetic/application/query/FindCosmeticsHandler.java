package br.com.muxe.core.common.cosmetic.application.query;

import br.com.muxe.core.common.cosmetic.domain.CosmeticProfile;
import br.com.muxe.core.common.cosmetic.domain.CosmeticRepository;
import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;

public final class FindCosmeticsHandler implements QueryHandler<FindCosmeticsQuery, CosmeticProfile> {

    private final CosmeticRepository cosmetics;
    private final Transactions transactions;

    public FindCosmeticsHandler(CosmeticRepository cosmetics, Transactions transactions) {
        this.cosmetics = cosmetics;
        this.transactions = transactions;
    }

    @Override
    public CosmeticProfile handle(FindCosmeticsQuery query) {
        return transactions.readOnly("cosmetics.find", () -> new CosmeticProfile(
            cosmetics.owned(query.playerId(), query.category()),
            cosmetics.selected(query.playerId(), query.category())
        ));
    }
}

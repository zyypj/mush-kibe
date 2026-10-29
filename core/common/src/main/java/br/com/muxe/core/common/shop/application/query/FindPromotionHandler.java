package br.com.muxe.core.common.shop.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.shop.domain.Promotion;
import br.com.muxe.core.common.shop.domain.PromotionRepository;
import java.util.Optional;

public final class FindPromotionHandler implements QueryHandler<FindPromotionQuery, Optional<Promotion>> {

    private final PromotionRepository promotions;
    private final Transactions transactions;

    public FindPromotionHandler(PromotionRepository promotions, Transactions transactions) {
        this.promotions = promotions;
        this.transactions = transactions;
    }

    @Override
    public Optional<Promotion> handle(FindPromotionQuery query) {
        return transactions.readOnly("shop.find_promotion", promotions::find);
    }
}

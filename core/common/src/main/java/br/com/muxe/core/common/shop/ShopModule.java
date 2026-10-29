package br.com.muxe.core.common.shop;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.shop.application.command.ChangePromotionCommand;
import br.com.muxe.core.common.shop.application.command.ChangePromotionHandler;
import br.com.muxe.core.common.shop.application.query.FindPromotionHandler;
import br.com.muxe.core.common.shop.application.query.FindPromotionQuery;
import br.com.muxe.core.common.shop.domain.PromotionRepository;
import java.time.Clock;

public final class ShopModule {

    private ShopModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, PromotionRepository promotions, Transactions transactions, Clock clock) {
        commands.register(ChangePromotionCommand.class, new ChangePromotionHandler(promotions, transactions, clock));
        queries.register(FindPromotionQuery.class, new FindPromotionHandler(promotions, transactions));
    }
}

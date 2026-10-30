package br.com.muxe.core.common.cosmetic;

import br.com.muxe.core.common.cosmetic.application.command.BuyCosmeticCommand;
import br.com.muxe.core.common.cosmetic.application.command.BuyCosmeticHandler;
import br.com.muxe.core.common.cosmetic.application.command.SelectCosmeticCommand;
import br.com.muxe.core.common.cosmetic.application.command.SelectCosmeticHandler;
import br.com.muxe.core.common.cosmetic.application.query.FindCosmeticsHandler;
import br.com.muxe.core.common.cosmetic.application.query.FindCosmeticsQuery;
import br.com.muxe.core.common.cosmetic.domain.CosmeticRepository;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class CosmeticModule {

    private CosmeticModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, CosmeticRepository cosmetics, CreditRepository credits, Transactions transactions, Clock clock) {
        commands.register(BuyCosmeticCommand.class, new BuyCosmeticHandler(cosmetics, credits, transactions, clock));
        commands.register(SelectCosmeticCommand.class, new SelectCosmeticHandler(cosmetics, transactions, clock));
        queries.register(FindCosmeticsQuery.class, new FindCosmeticsHandler(cosmetics, transactions));
    }
}

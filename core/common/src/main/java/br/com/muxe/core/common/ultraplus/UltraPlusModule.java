package br.com.muxe.core.common.ultraplus;

import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.ultraplus.application.command.ChangeUltraPlusStyleCommand;
import br.com.muxe.core.common.ultraplus.application.command.ChangeUltraPlusStyleHandler;
import br.com.muxe.core.common.ultraplus.application.command.ClaimUltraPlusRewardCommand;
import br.com.muxe.core.common.ultraplus.application.command.ClaimUltraPlusRewardHandler;
import br.com.muxe.core.common.ultraplus.application.query.FindUltraPlusHandler;
import br.com.muxe.core.common.ultraplus.application.query.FindUltraPlusQuery;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusRepository;
import java.time.Clock;

public final class UltraPlusModule {

    private UltraPlusModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, UltraPlusRepository profiles, CreditRepository credits, Transactions transactions, Clock clock) {
        commands.register(ChangeUltraPlusStyleCommand.class, new ChangeUltraPlusStyleHandler(profiles, transactions, clock));
        commands.register(ClaimUltraPlusRewardCommand.class, new ClaimUltraPlusRewardHandler(profiles, credits, transactions, clock));
        queries.register(FindUltraPlusQuery.class, new FindUltraPlusHandler(profiles, transactions));
    }
}

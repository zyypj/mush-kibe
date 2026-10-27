package br.com.muxe.core.common.shop.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.shop.domain.Promotion;
import br.com.muxe.core.common.shop.domain.PromotionRepository;
import java.time.Clock;

public final class ChangePromotionHandler implements CommandHandler<ChangePromotionCommand, Result<Promotion>> {

    private final PromotionRepository promotions;
    private final Transactions transactions;
    private final Clock clock;

    public ChangePromotionHandler(PromotionRepository promotions, Transactions transactions, Clock clock) {
        this.promotions = promotions;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Promotion> handle(ChangePromotionCommand command) {
        return transactions.inTransaction("shop.change_promotion", () -> {
            promotions.save(command.promotion(), clock.instant());
            return Result.success(command.promotion());
        });
    }
}

package br.com.muxe.core.common.cosmetic.application.command;

import br.com.muxe.core.common.cosmetic.domain.CosmeticRepository;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;

public final class SelectCosmeticHandler implements CommandHandler<SelectCosmeticCommand, Result<Void>> {

    private final CosmeticRepository cosmetics;
    private final Transactions transactions;
    private final Clock clock;

    public SelectCosmeticHandler(CosmeticRepository cosmetics, Transactions transactions, Clock clock) {
        this.cosmetics = cosmetics;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(SelectCosmeticCommand command) {
        return transactions.inTransaction("cosmetics.select", () -> {
            cosmetics.select(command.playerId(), command.category(), command.cosmeticId(), clock.instant());
            return Result.success(null);
        });
    }
}

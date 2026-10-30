package br.com.muxe.core.common.cosmetic.application.command;

import br.com.muxe.core.common.cosmetic.domain.CosmeticRepository;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;

public final class GrantCosmeticHandler implements CommandHandler<GrantCosmeticCommand, Result<Boolean>> {

    private final CosmeticRepository cosmetics;
    private final Transactions transactions;
    private final Clock clock;

    public GrantCosmeticHandler(CosmeticRepository cosmetics, Transactions transactions, Clock clock) {
        this.cosmetics = cosmetics;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Boolean> handle(GrantCosmeticCommand command) {
        return transactions.inTransaction("cosmetics.grant", () ->
            Result.success(cosmetics.own(command.playerId(), command.category(), command.cosmeticId(), command.source(), clock.instant())));
    }
}

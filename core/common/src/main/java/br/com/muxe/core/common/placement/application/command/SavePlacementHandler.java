package br.com.muxe.core.common.placement.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.PlacementRepository;
import br.com.muxe.core.common.result.Result;

public final class SavePlacementHandler implements CommandHandler<SavePlacementCommand, Result<Placement>> {

    private final PlacementRepository placements;
    private final Transactions transactions;

    public SavePlacementHandler(PlacementRepository placements, Transactions transactions) {
        this.placements = placements;
        this.transactions = transactions;
    }

    @Override
    public Result<Placement> handle(SavePlacementCommand command) {
        return transactions.inTransaction("placement.save", () -> {
            placements.save(command.placement());
            return Result.success(command.placement());
        });
    }
}

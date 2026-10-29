package br.com.muxe.core.common.placement.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.placement.domain.PlacementProblems;
import br.com.muxe.core.common.placement.domain.PlacementRepository;
import br.com.muxe.core.common.result.Result;

public final class RemovePlacementHandler implements CommandHandler<RemovePlacementCommand, Result<Void>> {

    private final PlacementRepository placements;
    private final Transactions transactions;

    public RemovePlacementHandler(PlacementRepository placements, Transactions transactions) {
        this.placements = placements;
        this.transactions = transactions;
    }

    @Override
    public Result<Void> handle(RemovePlacementCommand command) {
        return transactions.inTransaction("placement.remove", () -> placements.remove(command.kind(), command.group(), command.id())
            ? Result.success()
            : Result.failure(PlacementProblems.notFound(command.kind(), command.group(), command.id())));
    }
}

package br.com.muxe.lobby.visibility.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.lobby.visibility.domain.VisibilityRepository;
import java.time.Clock;

public final class ChangeVisibilityHandler implements CommandHandler<ChangeVisibilityCommand, Result<Void>> {

    private final VisibilityRepository settings;
    private final Transactions transactions;
    private final Clock clock;

    public ChangeVisibilityHandler(VisibilityRepository settings, Transactions transactions, Clock clock) {
        this.settings = settings;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ChangeVisibilityCommand command) {
        return transactions.inTransaction("lobby.change_visibility", () -> {
            settings.save(command.playerId(), command.visibility(), clock.instant());
            return Result.success();
        });
    }
}

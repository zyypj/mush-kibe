package br.com.muxe.core.common.account.application.command;

import br.com.muxe.core.common.account.domain.AccountDetailsRepository;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;

public final class SaveAccountLocationHandler implements CommandHandler<SaveAccountLocationCommand, Result<Void>> {

    private final AccountDetailsRepository details;
    private final Transactions transactions;
    private final Clock clock;

    public SaveAccountLocationHandler(AccountDetailsRepository details, Transactions transactions, Clock clock) {
        this.details = details;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(SaveAccountLocationCommand command) {
        return transactions.inTransaction("account.save_location", () -> {
            details.saveLocation(command.playerId(), command.location(), clock.instant());
            return Result.success();
        });
    }
}

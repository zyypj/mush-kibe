package br.com.muxe.core.common.credit.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.util.OptionalLong;

public final class ChangeCreditsHandler implements CommandHandler<ChangeCreditsCommand, Result<Long>> {

    public static final String NOT_ENOUGH = "credits.not_enough";

    private final CreditRepository credits;
    private final Transactions transactions;
    private final Clock clock;

    public ChangeCreditsHandler(CreditRepository credits, Transactions transactions, Clock clock) {
        this.credits = credits;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Long> handle(ChangeCreditsCommand command) {
        return transactions.inTransaction("credits.change", () -> {
            if (command.amount() > 0) {
                return Result.success(credits.add(command.playerId(), command.amount(), command.reason(), clock.instant()));
            }
            OptionalLong left = credits.spend(command.playerId(), -command.amount(), command.reason(), clock.instant());
            return left.isPresent()
                ? Result.success(left.getAsLong())
                : Result.failure(NOT_ENOUGH, "The player has fewer than " + -command.amount() + " credits");
        });
    }
}

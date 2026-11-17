package br.com.muxe.core.common.ultraplus.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusProfile;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusRepository;
import java.time.Clock;

public final class ChangeUltraPlusStyleHandler implements CommandHandler<ChangeUltraPlusStyleCommand, Result<Void>> {

    private final UltraPlusRepository profiles;
    private final Transactions transactions;
    private final Clock clock;

    public ChangeUltraPlusStyleHandler(UltraPlusRepository profiles, Transactions transactions, Clock clock) {
        this.profiles = profiles;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ChangeUltraPlusStyleCommand command) {
        return transactions.inTransaction("ultraplus.change_style", () -> {
            UltraPlusProfile profile = profiles.find(command.playerId()).orElseGet(() -> UltraPlusProfile.empty(command.playerId()));
            profiles.save(profile.withStyle(command.style()), clock.instant());
            return Result.success();
        });
    }
}

package br.com.muxe.core.common.preference.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.preference.domain.PreferenceRepository;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;

public final class ChangePreferenceHandler implements CommandHandler<ChangePreferenceCommand, Result<Void>> {

    public static final String INVALID_VALUE = "preference.invalid-value";

    private final PreferenceRepository preferences;
    private final Transactions transactions;
    private final Clock clock;

    public ChangePreferenceHandler(PreferenceRepository preferences, Transactions transactions, Clock clock) {
        this.preferences = preferences;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ChangePreferenceCommand command) {
        if (!command.preference().accepts(command.value())) {
            return Result.failure(INVALID_VALUE, "Preference " + command.preference().key() + " does not accept " + command.value());
        }
        return transactions.inTransaction("preference.change", () -> {
            preferences.save(command.playerId(), command.preference(), command.value(), clock.instant());
            return Result.success();
        });
    }
}

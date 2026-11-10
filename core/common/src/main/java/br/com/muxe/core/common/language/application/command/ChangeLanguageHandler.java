package br.com.muxe.core.common.language.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.language.domain.LanguageRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;

public final class ChangeLanguageHandler implements CommandHandler<ChangeLanguageCommand, Result<Void>> {

    private final LanguageRepository languages;
    private final Transactions transactions;
    private final Clock clock;

    public ChangeLanguageHandler(LanguageRepository languages, Transactions transactions, Clock clock) {
        this.languages = languages;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ChangeLanguageCommand command) {
        return transactions.inTransaction("language.change", () -> {
            languages.save(command.playerId(), command.language(), clock.instant());
            return Result.success();
        });
    }
}

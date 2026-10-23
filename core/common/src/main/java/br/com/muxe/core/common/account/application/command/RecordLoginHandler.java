package br.com.muxe.core.common.account.application.command;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountProblems;
import br.com.muxe.core.common.account.domain.AccountRepository;
import br.com.muxe.core.common.account.domain.Usernames;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public final class RecordLoginHandler implements CommandHandler<RecordLoginCommand, Result<Account>> {

    private final AccountRepository accounts;
    private final Transactions transactions;
    private final Clock clock;

    public RecordLoginHandler(AccountRepository accounts, Transactions transactions, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Account> handle(RecordLoginCommand command) {
        if (!Usernames.isValid(command.username())) {
            return Result.failure(AccountProblems.invalidUsername(command.username()));
        }
        return transactions.inTransaction("account.record_login", () -> {
            Optional<Account> holder = accounts.findByUsername(command.username());
            if (holder.isPresent() && !holder.get().id().equals(command.playerId())) {
                return Result.failure(AccountProblems.nameCaseConflict(command.username(), holder.get().username()));
            }
            Instant now = clock.instant();
            Account account = accounts.findById(command.playerId())
                .map(existing -> existing.withLogin(command.username(), now))
                .orElseGet(() -> Account.firstLogin(command.playerId(), command.username(), command.premium(), now));
            accounts.save(account);
            return Result.success(account);
        });
    }
}

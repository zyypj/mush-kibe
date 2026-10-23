package br.com.muxe.auth.credential.application.command;

import br.com.muxe.auth.credential.domain.AuthProblems;
import br.com.muxe.auth.credential.domain.Credential;
import br.com.muxe.auth.credential.domain.CredentialRepository;
import br.com.muxe.auth.credential.domain.PasswordHasher;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public final class VerifyCredentialHandler implements CommandHandler<VerifyCredentialCommand, Result<Void>> {

    private final CredentialRepository credentials;
    private final PasswordHasher hasher;
    private final Transactions transactions;
    private final Clock clock;

    public VerifyCredentialHandler(CredentialRepository credentials, PasswordHasher hasher, Transactions transactions, Clock clock) {
        this.credentials = credentials;
        this.hasher = hasher;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(VerifyCredentialCommand command) {
        Optional<Credential> found = transactions.readOnly("auth.find_credential", () -> credentials.find(command.playerId()));
        if (found.isEmpty()) {
            return Result.failure(AuthProblems.NOT_REGISTERED, "Player is not registered");
        }
        Credential credential = found.get();
        if (!hasher.matches(command.password(), credential.passwordHash())) {
            return Result.failure(AuthProblems.WRONG_PASSWORD, "Wrong password");
        }
        Instant now = clock.instant();
        Credential updated = credential.loggedIn(now);
        if (hasher.needsRehash(credential.passwordHash())) {
            updated = updated.withPasswordHash(hasher.hash(command.password()), now);
        }
        Credential toStore = updated;
        transactions.inTransaction("auth.record_login", () -> {
            credentials.update(toStore);
            return null;
        });
        return Result.success();
    }
}

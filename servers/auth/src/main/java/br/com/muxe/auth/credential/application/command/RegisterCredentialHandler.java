package br.com.muxe.auth.credential.application.command;

import br.com.muxe.auth.credential.domain.AuthProblems;
import br.com.muxe.auth.credential.domain.Credential;
import br.com.muxe.auth.credential.domain.CredentialRepository;
import br.com.muxe.auth.credential.domain.PasswordHash;
import br.com.muxe.auth.credential.domain.PasswordHasher;
import br.com.muxe.auth.credential.policy.PasswordPolicy;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Problem;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.util.Optional;

public final class RegisterCredentialHandler implements CommandHandler<RegisterCredentialCommand, Result<Void>> {

    private final CredentialRepository credentials;
    private final PasswordHasher hasher;
    private final PasswordPolicy policy;
    private final Transactions transactions;
    private final Clock clock;

    public RegisterCredentialHandler(CredentialRepository credentials, PasswordHasher hasher, PasswordPolicy policy, Transactions transactions, Clock clock) {
        this.credentials = credentials;
        this.hasher = hasher;
        this.policy = policy;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(RegisterCredentialCommand command) {
        Optional<Problem> invalid = policy.validate(command.username(), command.password(), command.confirmation());
        if (invalid.isPresent()) {
            return Result.failure(invalid.get());
        }
        if (transactions.readOnly("auth.check_registered", () -> credentials.exists(command.playerId()))) {
            return Result.failure(AuthProblems.ALREADY_REGISTERED, "Player is already registered");
        }
        PasswordHash hash = hasher.hash(command.password());
        return transactions.inTransaction("auth.register", () -> {
            if (credentials.exists(command.playerId())) {
                return Result.failure(AuthProblems.ALREADY_REGISTERED, "Player is already registered");
            }
            credentials.insert(Credential.register(command.playerId(), hash, clock.instant()));
            return Result.success();
        });
    }
}

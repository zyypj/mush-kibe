package br.com.muxe.auth.credential;

import br.com.muxe.auth.credential.application.command.RegisterCredentialCommand;
import br.com.muxe.auth.credential.application.command.RegisterCredentialHandler;
import br.com.muxe.auth.credential.application.command.VerifyCredentialCommand;
import br.com.muxe.auth.credential.application.command.VerifyCredentialHandler;
import br.com.muxe.auth.credential.application.query.HasCredentialHandler;
import br.com.muxe.auth.credential.application.query.HasCredentialQuery;
import br.com.muxe.auth.credential.domain.CredentialRepository;
import br.com.muxe.auth.credential.domain.PasswordHasher;
import br.com.muxe.auth.credential.policy.PasswordPolicy;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class CredentialModule {

    private CredentialModule() {
    }

    public static void register(
        CommandBus commands,
        QueryBus queries,
        CredentialRepository credentials,
        PasswordHasher hasher,
        PasswordPolicy policy,
        Transactions transactions,
        Clock clock
    ) {
        commands.register(RegisterCredentialCommand.class, new RegisterCredentialHandler(credentials, hasher, policy, transactions, clock));
        commands.register(VerifyCredentialCommand.class, new VerifyCredentialHandler(credentials, hasher, transactions, clock));
        queries.register(HasCredentialQuery.class, new HasCredentialHandler(credentials, transactions));
    }
}

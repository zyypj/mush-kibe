package br.com.muxe.auth.credential.application.query;

import br.com.muxe.auth.credential.domain.CredentialRepository;
import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;

public final class HasCredentialHandler implements QueryHandler<HasCredentialQuery, Boolean> {

    private final CredentialRepository credentials;
    private final Transactions transactions;

    public HasCredentialHandler(CredentialRepository credentials, Transactions transactions) {
        this.credentials = credentials;
        this.transactions = transactions;
    }

    @Override
    public Boolean handle(HasCredentialQuery query) {
        return transactions.readOnly("auth.has_credential", () -> credentials.exists(query.playerId()));
    }
}

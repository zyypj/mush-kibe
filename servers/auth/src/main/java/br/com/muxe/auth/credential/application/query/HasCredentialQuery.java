package br.com.muxe.auth.credential.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import java.util.Objects;
import java.util.UUID;

public record HasCredentialQuery(UUID playerId) implements Query<Boolean> {

    public HasCredentialQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

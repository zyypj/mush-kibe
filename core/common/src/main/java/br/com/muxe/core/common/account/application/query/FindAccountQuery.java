package br.com.muxe.core.common.account.application.query;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.cqrs.query.Query;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record FindAccountQuery(UUID playerId) implements Query<Optional<Account>> {

    public FindAccountQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

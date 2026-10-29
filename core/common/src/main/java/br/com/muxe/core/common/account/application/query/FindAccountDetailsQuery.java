package br.com.muxe.core.common.account.application.query;

import br.com.muxe.core.common.account.domain.AccountDetails;
import br.com.muxe.core.common.cqrs.query.Query;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record FindAccountDetailsQuery(Optional<UUID> id, Optional<String> username) implements Query<Optional<AccountDetails>> {

    public FindAccountDetailsQuery {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        if (id.isPresent() == username.isPresent()) {
            throw new IllegalArgumentException("Look an account up by id or by name");
        }
    }

    public static FindAccountDetailsQuery byId(UUID id) {
        return new FindAccountDetailsQuery(Optional.of(id), Optional.empty());
    }

    public static FindAccountDetailsQuery byUsername(String username) {
        return new FindAccountDetailsQuery(Optional.empty(), Optional.of(username));
    }
}

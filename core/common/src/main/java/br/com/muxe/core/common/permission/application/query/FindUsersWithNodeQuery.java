package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FindUsersWithNodeQuery(String key) implements Query<List<UUID>> {

    public FindUsersWithNodeQuery {
        Objects.requireNonNull(key, "key");
    }
}

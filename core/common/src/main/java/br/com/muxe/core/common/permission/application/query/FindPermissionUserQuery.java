package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import java.util.Objects;
import java.util.UUID;

public record FindPermissionUserQuery(UUID playerId) implements Query<PermissionUser> {

    public FindPermissionUserQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

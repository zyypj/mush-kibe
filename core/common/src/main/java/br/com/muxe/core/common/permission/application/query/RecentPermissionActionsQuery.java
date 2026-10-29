package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import java.util.List;

public record RecentPermissionActionsQuery(int limit) implements Query<List<PermissionAction>> {

    public RecentPermissionActionsQuery {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("The action limit goes from 1 to 500, got: " + limit);
        }
    }
}

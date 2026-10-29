package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.List;

public final class RecentPermissionActionsHandler implements QueryHandler<RecentPermissionActionsQuery, List<PermissionAction>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;

    public RecentPermissionActionsHandler(PermissionRepository permissions, Transactions transactions) {
        this.permissions = permissions;
        this.transactions = transactions;
    }

    @Override
    public List<PermissionAction> handle(RecentPermissionActionsQuery query) {
        return transactions.readOnly("permission.recent_actions", () -> permissions.recentActions(query.limit()));
    }
}

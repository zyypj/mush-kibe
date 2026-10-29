package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.List;
import java.util.UUID;

public final class FindUsersWithNodeHandler implements QueryHandler<FindUsersWithNodeQuery, List<UUID>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;

    public FindUsersWithNodeHandler(PermissionRepository permissions, Transactions transactions) {
        this.permissions = permissions;
        this.transactions = transactions;
    }

    @Override
    public List<UUID> handle(FindUsersWithNodeQuery query) {
        return transactions.readOnly("permission.find_users_with_node", () -> permissions.usersWith(query.key()));
    }
}

package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.persistence.transaction.Transactions;

public final class FindPermissionUserHandler implements QueryHandler<FindPermissionUserQuery, PermissionUser> {

    private final PermissionRepository permissions;
    private final Transactions transactions;

    public FindPermissionUserHandler(PermissionRepository permissions, Transactions transactions) {
        this.permissions = permissions;
        this.transactions = transactions;
    }

    @Override
    public PermissionUser handle(FindPermissionUserQuery query) {
        return transactions.readOnly("permission.find_user", () -> permissions.user(query.playerId()));
    }
}

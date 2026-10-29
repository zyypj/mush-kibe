package br.com.muxe.core.common.permission.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;

public final class LoadPermissionDataHandler implements QueryHandler<LoadPermissionDataQuery, PermissionData> {

    private final PermissionRepository permissions;
    private final Transactions transactions;

    public LoadPermissionDataHandler(PermissionRepository permissions, Transactions transactions) {
        this.permissions = permissions;
        this.transactions = transactions;
    }

    @Override
    public PermissionData handle(LoadPermissionDataQuery query) {
        return transactions.readOnly("permission.load", permissions::load);
    }
}

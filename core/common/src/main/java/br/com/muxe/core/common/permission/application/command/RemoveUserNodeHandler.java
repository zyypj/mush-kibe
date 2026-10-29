package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;

public final class RemoveUserNodeHandler implements CommandHandler<RemoveUserNodeCommand, Result<PermissionUser>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;
    private final Clock clock;

    public RemoveUserNodeHandler(PermissionRepository permissions, Transactions transactions, Clock clock) {
        this.permissions = permissions;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<PermissionUser> handle(RemoveUserNodeCommand command) {
        return transactions.inTransaction("permission.remove_user_node", () -> {
            PermissionUser current = permissions.user(command.playerId());
            PermissionUser next = current.without(command.key(), command.contexts());
            if (next.nodes().size() == current.nodes().size()) {
                return Result.<PermissionUser>failure(PermissionProblems.NODE_MISSING, "The user does not have " + command.key());
            }
            permissions.saveUser(next);
            permissions.log(new PermissionAction(clock.instant(), command.actor(), "removed " + command.key() + " from user " + command.playerId()));
            return Result.success(next);
        });
    }
}

package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.UserGrants;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;

public final class AddUserNodeHandler implements CommandHandler<AddUserNodeCommand, Result<PermissionUser>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;
    private final Clock clock;
    private final UserGrants grants;

    public AddUserNodeHandler(PermissionRepository permissions, Transactions transactions, Clock clock) {
        this(permissions, transactions, clock, UserGrants.NONE);
    }

    public AddUserNodeHandler(PermissionRepository permissions, Transactions transactions, Clock clock, UserGrants grants) {
        this.permissions = permissions;
        this.transactions = transactions;
        this.clock = clock;
        this.grants = grants;
    }

    @Override
    public Result<PermissionUser> handle(AddUserNodeCommand command) {
        return transactions.inTransaction("permission.add_user_node", () -> {
            if (command.node().isGroup() && permissions.load().group(command.node().groupId()).isEmpty()) {
                return Result.<PermissionUser>failure(PermissionProblems.UNKNOWN_GROUP, "The group '" + command.node().groupId() + "' does not exist");
            }
            PermissionUser before = permissions.user(command.playerId());
            PermissionUser user = before.with(command.node());
            permissions.saveUser(user);
            if (grants.concerns(user)) {
                grants.changed(before, user, clock.instant());
            }
            String until = command.node().expiresAt().map(at -> " until " + at).orElse("");
            permissions.log(new PermissionAction(clock.instant(), command.actor(),
                "set " + command.node().key() + "=" + command.node().value() + until + " on user " + command.playerId()));
            return Result.success(user);
        });
    }
}

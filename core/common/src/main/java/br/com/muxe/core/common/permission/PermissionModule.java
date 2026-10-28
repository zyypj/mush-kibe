package br.com.muxe.core.common.permission;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.permission.application.command.AddUserNodeCommand;
import br.com.muxe.core.common.permission.application.command.AddUserNodeHandler;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesCommand;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesHandler;
import br.com.muxe.core.common.permission.application.command.RemoveUserNodeCommand;
import br.com.muxe.core.common.permission.application.command.RemoveUserNodeHandler;
import br.com.muxe.core.common.permission.application.command.SelectMedalCommand;
import br.com.muxe.core.common.permission.application.command.SelectMedalHandler;
import br.com.muxe.core.common.permission.application.command.SelectTagCommand;
import br.com.muxe.core.common.permission.application.command.SelectTagHandler;
import br.com.muxe.core.common.permission.application.query.FindPermissionUserHandler;
import br.com.muxe.core.common.permission.application.query.FindPermissionUserQuery;
import br.com.muxe.core.common.permission.application.query.FindUsersWithNodeHandler;
import br.com.muxe.core.common.permission.application.query.FindUsersWithNodeQuery;
import br.com.muxe.core.common.permission.application.query.LoadPermissionDataHandler;
import br.com.muxe.core.common.permission.application.query.LoadPermissionDataQuery;
import br.com.muxe.core.common.permission.application.query.RecentPermissionActionsHandler;
import br.com.muxe.core.common.permission.application.query.RecentPermissionActionsQuery;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class PermissionModule {

    private PermissionModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, PermissionRepository permissions, Transactions transactions, Clock clock) {
        commands.register(ApplyPermissionChangesCommand.class, new ApplyPermissionChangesHandler(permissions, transactions, clock));
        commands.register(AddUserNodeCommand.class, new AddUserNodeHandler(permissions, transactions, clock));
        commands.register(RemoveUserNodeCommand.class, new RemoveUserNodeHandler(permissions, transactions, clock));
        commands.register(SelectTagCommand.class, new SelectTagHandler(permissions, transactions));
        commands.register(SelectMedalCommand.class, new SelectMedalHandler(permissions, transactions));
        queries.register(LoadPermissionDataQuery.class, new LoadPermissionDataHandler(permissions, transactions));
        queries.register(FindPermissionUserQuery.class, new FindPermissionUserHandler(permissions, transactions));
        queries.register(FindUsersWithNodeQuery.class, new FindUsersWithNodeHandler(permissions, transactions));
        queries.register(RecentPermissionActionsQuery.class, new RecentPermissionActionsHandler(permissions, transactions));
    }
}

package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.permission.domain.PermissionRules;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.UserGrants;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Problem;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ApplyPermissionChangesHandler implements CommandHandler<ApplyPermissionChangesCommand, Result<Void>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;
    private final Clock clock;
    private final UserGrants grants;

    public ApplyPermissionChangesHandler(PermissionRepository permissions, Transactions transactions, Clock clock) {
        this(permissions, transactions, clock, UserGrants.NONE);
    }

    public ApplyPermissionChangesHandler(PermissionRepository permissions, Transactions transactions, Clock clock, UserGrants grants) {
        this.permissions = permissions;
        this.transactions = transactions;
        this.clock = clock;
        this.grants = grants;
    }

    @Override
    public Result<Void> handle(ApplyPermissionChangesCommand command) {
        return transactions.inTransaction("permission.apply_changes", () -> {
            PermissionData next = PermissionChange.applyAll(permissions.load(), command.changes());
            Optional<Problem> problem = PermissionRules.check(next);
            if (problem.isEmpty()) {
                problem = command.changes().stream()
                    .filter(PermissionChange.PutUser.class::isInstance)
                    .map(change -> PermissionRules.checkUser(next, ((PermissionChange.PutUser) change).user()))
                    .flatMap(Optional::stream)
                    .findFirst();
            }
            if (problem.isPresent()) {
                return Result.<Void>failure(problem.get());
            }
            Instant now = clock.instant();
            Map<UUID, PermissionUser> users = new LinkedHashMap<>();
            List<PermissionAction> actions = new ArrayList<>();
            for (PermissionChange change : command.changes()) {
                switch (change) {
                    case PermissionChange.PutTag put -> permissions.saveTag(put.tag());
                    case PermissionChange.PutMedal put -> permissions.saveMedal(put.medal());
                    case PermissionChange.DeleteMedal delete -> permissions.deleteMedal(delete.id());
                    case PermissionChange.PutGroup put -> permissions.saveGroup(put.group());
                    case PermissionChange.PutTrack put -> permissions.saveTrack(put.track());
                    case PermissionChange.PutUser put -> users.put(put.user().id(), put.user());
                    case PermissionChange.DeleteTrack delete -> permissions.deleteTrack(delete.id());
                    case PermissionChange.DeleteGroup delete -> permissions.deleteGroup(delete.id());
                    case PermissionChange.DeleteTag delete -> permissions.deleteTag(delete.id());
                }
                actions.add(new PermissionAction(now, command.actor(), change.describe()));
            }
            List<PermissionUser> before = users.values().stream().filter(grants::concerns).map(user -> permissions.user(user.id())).toList();
            permissions.saveUsers(users.values());
            before.forEach(previous -> grants.changed(previous, users.get(previous.id()), now));
            permissions.logAll(actions);
            return Result.success();
        });
    }
}

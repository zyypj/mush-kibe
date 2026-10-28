package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.permission.domain.PermissionRules;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Problem;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public final class ApplyPermissionChangesHandler implements CommandHandler<ApplyPermissionChangesCommand, Result<Void>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;
    private final Clock clock;

    public ApplyPermissionChangesHandler(PermissionRepository permissions, Transactions transactions, Clock clock) {
        this.permissions = permissions;
        this.transactions = transactions;
        this.clock = clock;
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
            for (PermissionChange change : command.changes()) {
                switch (change) {
                    case PermissionChange.PutTag put -> permissions.saveTag(put.tag());
                    case PermissionChange.PutMedal put -> permissions.saveMedal(put.medal());
                    case PermissionChange.DeleteMedal delete -> permissions.deleteMedal(delete.id());
                    case PermissionChange.PutGroup put -> permissions.saveGroup(put.group());
                    case PermissionChange.PutTrack put -> permissions.saveTrack(put.track());
                    case PermissionChange.PutUser put -> permissions.saveUser(put.user());
                    case PermissionChange.DeleteTrack delete -> permissions.deleteTrack(delete.id());
                    case PermissionChange.DeleteGroup delete -> permissions.deleteGroup(delete.id());
                    case PermissionChange.DeleteTag delete -> permissions.deleteTag(delete.id());
                }
                permissions.log(new PermissionAction(now, command.actor(), change.describe()));
            }
            return Result.success();
        });
    }
}

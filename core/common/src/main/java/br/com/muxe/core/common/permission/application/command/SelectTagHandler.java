package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;

public final class SelectTagHandler implements CommandHandler<SelectTagCommand, Result<Void>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;

    public SelectTagHandler(PermissionRepository permissions, Transactions transactions) {
        this.permissions = permissions;
        this.transactions = transactions;
    }

    @Override
    public Result<Void> handle(SelectTagCommand command) {
        return transactions.inTransaction("permission.select_tag", () -> {
            if (command.tag().isPresent() && permissions.load().tag(command.tag().get()).isEmpty()) {
                return Result.<Void>failure(PermissionProblems.UNKNOWN_TAG, "The tag '" + command.tag().get() + "' does not exist");
            }
            permissions.selectTag(command.playerId(), command.tag());
            return Result.success();
        });
    }
}

package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;

public final class SelectMedalHandler implements CommandHandler<SelectMedalCommand, Result<Void>> {

    private final PermissionRepository permissions;
    private final Transactions transactions;

    public SelectMedalHandler(PermissionRepository permissions, Transactions transactions) {
        this.permissions = permissions;
        this.transactions = transactions;
    }

    @Override
    public Result<Void> handle(SelectMedalCommand command) {
        return transactions.inTransaction("permission.select_medal", () -> {
            if (command.medal().isPresent() && permissions.load().medal(command.medal().get()).isEmpty()) {
                return Result.<Void>failure(PermissionProblems.UNKNOWN_MEDAL, "The medal '" + command.medal().get() + "' does not exist");
            }
            permissions.selectMedal(command.playerId(), command.medal());
            return Result.success();
        });
    }
}

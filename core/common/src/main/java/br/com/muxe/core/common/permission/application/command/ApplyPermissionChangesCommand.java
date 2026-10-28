package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.Objects;

public record ApplyPermissionChangesCommand(String actor, List<PermissionChange> changes) implements Command<Result<Void>> {

    public ApplyPermissionChangesCommand {
        Objects.requireNonNull(actor, "actor");
        changes = List.copyOf(changes);
    }
}

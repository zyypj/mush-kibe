package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record RemoveUserNodeCommand(String actor, UUID playerId, String key, Contexts contexts) implements Command<Result<PermissionUser>> {

    public RemoveUserNodeCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contexts, "contexts");
    }
}

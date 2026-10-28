package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record AddUserNodeCommand(String actor, UUID playerId, PermissionNode node) implements Command<Result<PermissionUser>> {

    public AddUserNodeCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(node, "node");
    }
}

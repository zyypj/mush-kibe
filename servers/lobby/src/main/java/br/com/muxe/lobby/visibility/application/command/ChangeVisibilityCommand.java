package br.com.muxe.lobby.visibility.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.lobby.visibility.domain.Visibility;
import java.util.Objects;
import java.util.UUID;

public record ChangeVisibilityCommand(UUID playerId, Visibility visibility) implements Command<Result<Void>> {

    public ChangeVisibilityCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(visibility, "visibility");
    }
}

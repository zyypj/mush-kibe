package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SelectTagCommand(UUID playerId, Optional<String> tag) implements Command<Result<Void>> {

    public SelectTagCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(tag, "tag");
    }
}

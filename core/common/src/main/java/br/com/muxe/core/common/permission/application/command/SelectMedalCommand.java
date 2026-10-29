package br.com.muxe.core.common.permission.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SelectMedalCommand(UUID playerId, Optional<String> medal) implements Command<Result<Void>> {

    public SelectMedalCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(medal, "medal");
    }
}

package br.com.muxe.core.common.account.application.command;

import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record SaveAccountLocationCommand(UUID playerId, AccountLocation location) implements Command<Result<Void>> {

    public SaveAccountLocationCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(location, "location");
    }
}

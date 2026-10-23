package br.com.muxe.core.common.account.application.command;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record RecordLoginCommand(UUID playerId, String username) implements Command<Result<Account>> {

    public RecordLoginCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(username, "username");
    }
}

package br.com.muxe.auth.credential.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record RegisterCredentialCommand(UUID playerId, String username, String password, String confirmation) implements Command<Result<Void>> {

    public RegisterCredentialCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(confirmation, "confirmation");
    }

    @Override
    public String toString() {
        return "RegisterCredentialCommand[playerId=" + playerId + ", username=" + username + "]";
    }
}

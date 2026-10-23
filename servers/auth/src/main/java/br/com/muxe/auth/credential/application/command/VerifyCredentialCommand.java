package br.com.muxe.auth.credential.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record VerifyCredentialCommand(UUID playerId, String password) implements Command<Result<Void>> {

    public VerifyCredentialCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(password, "password");
    }

    @Override
    public String toString() {
        return "VerifyCredentialCommand[playerId=" + playerId + "]";
    }
}

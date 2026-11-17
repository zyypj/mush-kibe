package br.com.muxe.core.common.ultraplus.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.util.Objects;
import java.util.UUID;

public record ChangeUltraPlusStyleCommand(UUID playerId, UltraPlusStyle style) implements Command<Result<Void>> {

    public ChangeUltraPlusStyleCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(style, "style");
    }
}

package br.com.muxe.core.common.ultraplus.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public record ClaimUltraPlusRewardCommand(UUID playerId, YearMonth month, long credits) implements Command<Result<Void>> {

    public ClaimUltraPlusRewardCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(month, "month");
        if (credits < 0) {
            throw new IllegalArgumentException("The reward cannot be negative, got: " + credits);
        }
    }
}

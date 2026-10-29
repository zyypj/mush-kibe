package br.com.muxe.core.common.parkour.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.parkour.domain.ParkourOutcome;
import br.com.muxe.core.common.result.Result;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record SubmitParkourTimeCommand(String group, UUID playerId, Duration time) implements Command<Result<ParkourOutcome>> {

    public SubmitParkourTimeCommand {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(time, "time");
        if (time.isNegative()) {
            throw new IllegalArgumentException("A parkour time cannot be negative");
        }
    }
}

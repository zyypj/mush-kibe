package br.com.muxe.core.common.placement.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;

public record SavePlacementCommand(Placement placement) implements Command<Result<Placement>> {

    public SavePlacementCommand {
        Objects.requireNonNull(placement, "placement");
    }
}

package br.com.muxe.core.common.placement.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;

public record RemovePlacementCommand(PlacementKind kind, String group, String id) implements Command<Result<Void>> {

    public RemovePlacementCommand {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(id, "id");
    }
}

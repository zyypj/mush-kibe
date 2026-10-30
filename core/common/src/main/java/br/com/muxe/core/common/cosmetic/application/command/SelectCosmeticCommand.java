package br.com.muxe.core.common.cosmetic.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record SelectCosmeticCommand(UUID playerId, String category, String cosmeticId) implements Command<Result<Void>> {

    public SelectCosmeticCommand {
        Objects.requireNonNull(playerId, "playerId");
        CosmeticIds.require(category, cosmeticId);
    }
}

package br.com.muxe.core.common.cosmetic.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record BuyCosmeticCommand(UUID playerId, String category, String cosmeticId, long price) implements Command<Result<Long>> {

    public BuyCosmeticCommand {
        Objects.requireNonNull(playerId, "playerId");
        CosmeticIds.require(category, cosmeticId);
        if (price < 0) {
            throw new IllegalArgumentException("A cosmetic cannot cost " + price);
        }
    }
}

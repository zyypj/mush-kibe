package br.com.muxe.core.common.cosmetic.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record GrantCosmeticCommand(UUID playerId, String category, String cosmeticId, String source) implements Command<Result<Boolean>> {

    private static final Pattern SOURCE = Pattern.compile("[a-z0-9_.:-]{1,32}");

    public GrantCosmeticCommand {
        Objects.requireNonNull(playerId, "playerId");
        CosmeticIds.require(category, cosmeticId);
        if (source == null || !SOURCE.matcher(source).matches()) {
            throw new IllegalArgumentException("Invalid cosmetic source: " + source);
        }
    }
}

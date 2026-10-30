package br.com.muxe.core.common.credit.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record ChangeCreditsCommand(UUID playerId, long amount, String reason) implements Command<Result<Long>> {

    private static final Pattern REASON = Pattern.compile("[a-z0-9_.:-]{1,64}");

    public ChangeCreditsCommand {
        Objects.requireNonNull(playerId, "playerId");
        if (amount == 0) {
            throw new IllegalArgumentException("A credit change needs an amount");
        }
        if (reason == null || !REASON.matcher(reason).matches()) {
            throw new IllegalArgumentException("Invalid credit reason: " + reason);
        }
    }
}

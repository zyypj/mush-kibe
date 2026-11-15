package br.com.muxe.core.common.preference.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.preference.domain.Preference;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record ChangePreferenceCommand(UUID playerId, Preference preference, String value) implements Command<Result<Void>> {

    public ChangePreferenceCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(preference, "preference");
        Objects.requireNonNull(value, "value");
    }
}

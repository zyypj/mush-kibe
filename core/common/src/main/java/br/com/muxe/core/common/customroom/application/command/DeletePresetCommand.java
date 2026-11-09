package br.com.muxe.core.common.customroom.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import java.util.Objects;
import java.util.UUID;

public record DeletePresetCommand(UUID owner, UUID preset) implements Command<Boolean> {

    public DeletePresetCommand {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(preset, "preset");
    }
}

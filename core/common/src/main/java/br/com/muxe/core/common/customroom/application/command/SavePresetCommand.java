package br.com.muxe.core.common.customroom.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SavePresetCommand(UUID owner, String game, String name, String mode, List<String> maps, RoomOptions options) implements Command<Result<RoomPreset>> {

    public SavePresetCommand {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(options, "options");
        maps = List.copyOf(maps);
    }
}

package br.com.muxe.core.common.language.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.result.Result;
import java.util.Objects;
import java.util.UUID;

public record ChangeLanguageCommand(UUID playerId, Language language) implements Command<Result<Void>> {

    public ChangeLanguageCommand {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(language, "language");
    }
}

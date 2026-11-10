package br.com.muxe.core.common.language.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.language.domain.Language;
import java.util.Objects;
import java.util.UUID;

public record FindLanguageQuery(UUID playerId) implements Query<Language> {

    public FindLanguageQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

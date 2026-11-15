package br.com.muxe.core.common.preference.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.preference.domain.Preferences;
import java.util.Objects;
import java.util.UUID;

public record FindPreferencesQuery(UUID playerId) implements Query<Preferences> {

    public FindPreferencesQuery {
        Objects.requireNonNull(playerId, "playerId");
    }
}

package br.com.muxe.core.common.parkour.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record FindParkourRecordQuery(String group, UUID playerId) implements Query<Optional<Duration>> {

    public FindParkourRecordQuery {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(playerId, "playerId");
    }
}

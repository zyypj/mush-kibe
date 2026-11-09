package br.com.muxe.core.common.customroom.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ListPresetsQuery(UUID owner, String game) implements Query<List<RoomPreset>> {

    public ListPresetsQuery {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(game, "game");
    }
}

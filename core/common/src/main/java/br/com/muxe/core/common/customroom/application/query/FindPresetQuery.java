package br.com.muxe.core.common.customroom.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import java.util.Objects;
import java.util.Optional;

public record FindPresetQuery(String code) implements Query<Optional<RoomPreset>> {

    public FindPresetQuery {
        Objects.requireNonNull(code, "code");
    }
}

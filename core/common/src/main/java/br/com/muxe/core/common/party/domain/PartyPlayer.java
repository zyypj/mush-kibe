package br.com.muxe.core.common.party.domain;

import java.util.Objects;
import java.util.UUID;

public record PartyPlayer(UUID id, String name) {

    public PartyPlayer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}

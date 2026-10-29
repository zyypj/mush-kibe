package br.com.muxe.core.common.permission.domain;

import java.time.Instant;
import java.util.Objects;

public record PermissionAction(Instant at, String actor, String description) {

    public PermissionAction {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(description, "description");
    }
}

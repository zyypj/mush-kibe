package br.com.muxe.core.common.permission.editor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EditorSession(UUID actorId, String actorName, Instant createdAt) {

    public EditorSession {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(actorName, "actorName");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}

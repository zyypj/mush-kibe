package br.com.muxe.auth.credential.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Credential(UUID playerId, PasswordHash passwordHash, Instant createdAt, Instant updatedAt, Instant lastLoginAt) {

    public Credential {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Credential register(UUID playerId, PasswordHash passwordHash, Instant at) {
        return new Credential(playerId, passwordHash, at, at, at);
    }

    public Credential loggedIn(Instant at) {
        return new Credential(playerId, passwordHash, createdAt, updatedAt, at);
    }

    public Credential withPasswordHash(PasswordHash hash, Instant at) {
        return new Credential(playerId, hash, createdAt, at, lastLoginAt);
    }
}

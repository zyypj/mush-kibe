package br.com.muxe.core.common.account.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record Account(UUID id, String username, boolean premium, Instant firstSeenAt, Instant lastSeenAt) {

    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(firstSeenAt, "firstSeenAt");
        Objects.requireNonNull(lastSeenAt, "lastSeenAt");
        if (username != null && !Usernames.isValid(username)) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }
    }

    public static Account firstLogin(UUID id, String username, boolean premium, Instant at) {
        Objects.requireNonNull(username, "username");
        return new Account(id, username, premium, at, at);
    }

    public Account withLogin(String currentUsername, Instant at) {
        Objects.requireNonNull(currentUsername, "currentUsername");
        return new Account(id, currentUsername, premium, firstSeenAt, at);
    }

    public Account withoutUsername() {
        return new Account(id, null, premium, firstSeenAt, lastSeenAt);
    }

    public Optional<String> currentUsername() {
        return Optional.ofNullable(username);
    }
}

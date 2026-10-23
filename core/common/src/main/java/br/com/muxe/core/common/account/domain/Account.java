package br.com.muxe.core.common.account.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Account(UUID id, String username, boolean premium, Instant firstSeenAt, Instant lastSeenAt) {

    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(firstSeenAt, "firstSeenAt");
        Objects.requireNonNull(lastSeenAt, "lastSeenAt");
        if (!Usernames.isValid(username)) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }
    }

    public static Account firstLogin(UUID id, String username, boolean premium, Instant at) {
        return new Account(id, username, premium, at, at);
    }

    public Account withLogin(String currentUsername, Instant at) {
        return new Account(id, currentUsername, premium, firstSeenAt, at);
    }
}

package br.com.muxe.core.common.permission.editor;

import br.com.muxe.core.common.store.ExpiringStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

public final class EditorSessions {

    public static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);
    public static final String PERMISSION = "muxe.permissions.editor";
    public static final UUID CONSOLE = new UUID(0L, 0L);

    private static final String KEY_PREFIX = "permissions:editor:";
    private static final String SEPARATOR = "|";
    private static final int TOKEN_BYTES = 32;

    private final ExpiringStore store;
    private final Clock clock;
    private final Duration duration;
    private final SecureRandom random = new SecureRandom();

    public EditorSessions(ExpiringStore store, Clock clock, Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Editor sessions need a positive duration");
        }
        this.store = store;
        this.clock = clock;
        this.duration = duration;
    }

    public String open(UUID actorId, String actorName) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store.put(key(token), actorId + SEPARATOR + actorName + SEPARATOR + clock.instant().toEpochMilli(), duration);
        return token;
    }

    public Optional<EditorSession> find(String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            return Optional.empty();
        }
        return store.get(key(token)).flatMap(EditorSessions::parse);
    }

    public void close(String token) {
        if (token != null && !token.isBlank()) {
            store.delete(key(token));
        }
    }

    public Duration duration() {
        return duration;
    }

    private static Optional<EditorSession> parse(String value) {
        String[] parts = value.split("\\" + SEPARATOR, 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new EditorSession(UUID.fromString(parts[0]), parts[1], Instant.ofEpochMilli(Long.parseLong(parts[2]))));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String key(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

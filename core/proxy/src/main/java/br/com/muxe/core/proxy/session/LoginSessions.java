package br.com.muxe.core.proxy.session;

import br.com.muxe.core.common.store.ExpiringStore;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

public final class LoginSessions {

    private static final String KEY_PREFIX = "auth:session:";

    private final ExpiringStore store;
    private final Duration duration;

    public LoginSessions(ExpiringStore store, Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Session duration must be positive");
        }
        this.store = store;
        this.duration = duration;
    }

    public void remember(UUID playerId, InetAddress address) {
        store.put(key(playerId), fingerprint(playerId, address), duration);
    }

    public boolean canResume(UUID playerId, InetAddress address) {
        return store.get(key(playerId)).map(fingerprint(playerId, address)::equals).orElse(false);
    }

    public void forget(UUID playerId) {
        store.delete(key(playerId));
    }

    private static String key(UUID playerId) {
        return KEY_PREFIX + playerId;
    }

    private static String fingerprint(UUID playerId, InetAddress address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((playerId + "|" + address.getHostAddress()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

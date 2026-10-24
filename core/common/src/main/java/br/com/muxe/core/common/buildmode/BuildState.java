package br.com.muxe.core.common.buildmode;

import br.com.muxe.core.common.store.ExpiringStore;
import java.time.Duration;
import java.util.UUID;

public final class BuildState {

    private static final String KEY_PREFIX = "build:";
    private static final String ON = "on";

    private final ExpiringStore store;
    private final Duration duration;

    public BuildState(ExpiringStore store, Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Build mode must last a positive time");
        }
        this.store = store;
        this.duration = duration;
    }

    public boolean isOn(UUID playerId) {
        return store.get(key(playerId)).isPresent();
    }

    public void turnOn(UUID playerId) {
        store.put(key(playerId), ON, duration);
    }

    public void turnOff(UUID playerId) {
        store.delete(key(playerId));
    }

    private static String key(UUID playerId) {
        return KEY_PREFIX + playerId;
    }
}

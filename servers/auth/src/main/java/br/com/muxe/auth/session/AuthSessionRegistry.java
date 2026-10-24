package br.com.muxe.auth.session;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthSessionRegistry {

    private final Map<UUID, AuthState> states = new ConcurrentHashMap<>();

    public void set(UUID playerId, AuthState state) {
        states.put(playerId, state);
    }

    public boolean transition(UUID playerId, AuthState expected, AuthState next) {
        return states.replace(playerId, expected, next);
    }

    public Optional<AuthState> state(UUID playerId) {
        return Optional.ofNullable(states.get(playerId));
    }

    public boolean isAuthenticated(UUID playerId) {
        return states.get(playerId) == AuthState.AUTHENTICATED;
    }

    public Map<UUID, AuthState> snapshot() {
        return Map.copyOf(states);
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }
}

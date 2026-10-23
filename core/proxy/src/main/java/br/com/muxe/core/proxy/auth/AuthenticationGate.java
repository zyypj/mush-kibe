package br.com.muxe.core.proxy.auth;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthenticationGate {

    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();

    public void authenticate(UUID playerId) {
        authenticated.add(playerId);
    }

    public boolean isAuthenticated(UUID playerId) {
        return authenticated.contains(playerId);
    }

    public void forget(UUID playerId) {
        authenticated.remove(playerId);
    }

    public int size() {
        return authenticated.size();
    }
}

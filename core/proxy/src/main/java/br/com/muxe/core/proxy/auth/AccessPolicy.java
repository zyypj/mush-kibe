package br.com.muxe.core.proxy.auth;

import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import java.util.UUID;

public final class AccessPolicy {

    private final AuthenticationGate gate;
    private final ServerDirectory directory;
    private final String authGroup;

    public AccessPolicy(AuthenticationGate gate, ServerDirectory directory, String authGroup) {
        this.gate = gate;
        this.directory = directory;
        this.authGroup = authGroup;
    }

    public boolean mayConnect(UUID playerId, String serverId) {
        return gate.isAuthenticated(playerId) || isAuthServer(serverId);
    }

    public boolean isAuthServer(String serverId) {
        return directory.find(serverId)
            .map(status -> status.identity().group().equals(authGroup))
            .orElse(false);
    }

    public boolean isTrustedAuthOrigin(String serverId) {
        return directory.find(serverId)
            .map(status -> status.identity().kind() == ServerKind.AUTH && status.identity().group().equals(authGroup))
            .orElse(false);
    }
}

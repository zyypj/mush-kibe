package br.com.muxe.core.common.server.directory;

import br.com.muxe.core.common.server.ServerIdentity;
import java.time.Instant;

public record ServerStatus(
    ServerIdentity identity,
    int onlinePlayers,
    int maxPlayers,
    boolean joinable,
    Instant reportedAt,
    Instant receivedAt
) {

    public boolean hasRoom() {
        return joinable && onlinePlayers < maxPlayers;
    }
}

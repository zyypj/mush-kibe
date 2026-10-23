package br.com.muxe.core.common.server.heartbeat;

public interface ServerLoad {

    int onlinePlayers();

    int maxPlayers();

    default boolean joinable() {
        return true;
    }
}

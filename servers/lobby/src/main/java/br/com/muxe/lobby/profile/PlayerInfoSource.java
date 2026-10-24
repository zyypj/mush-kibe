package br.com.muxe.lobby.profile;

import java.util.UUID;

@FunctionalInterface
public interface PlayerInfoSource {

    PlayerInfo of(UUID playerId);

    static PlayerInfoSource fixed(PlayerInfo info) {
        return playerId -> info;
    }
}

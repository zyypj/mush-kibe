package br.com.muxe.lobby.profile;

import java.util.Objects;

public record PlayerInfo(String tag, long credits) {

    public PlayerInfo {
        Objects.requireNonNull(tag, "tag");
    }
}

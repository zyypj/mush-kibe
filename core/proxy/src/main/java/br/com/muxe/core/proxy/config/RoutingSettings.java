package br.com.muxe.core.proxy.config;

import br.com.muxe.core.common.config.source.ConfigSource;

public record RoutingSettings(String authGroup, String lobbyGroup) {

    public static RoutingSettings from(ConfigSource source) {
        return new RoutingSettings(
            source.string("routing.auth-group", "auth"),
            source.string("routing.lobby-group", "lobby")
        );
    }
}

package br.com.muxe.core.proxy.routing;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.proxy.config.RoutingSettings;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class InitialRoutesTest {

    private final RoutingSettings routing = new RoutingSettings("auth", "lobby");

    @Test
    void authenticatedPlayersStartInTheLobby() {
        InitialRoute<String> route = InitialRoutes.choose(true, routing, alive(Map.of("lobby", "lobby-1", "auth", "auth-1")));

        assertThat(route).isEqualTo(new InitialRoute.Join<>("lobby-1", false));
    }

    @Test
    void authenticatedPlayersWaitOnTheAuthServerWhenNoLobbyIsAlive() {
        InitialRoute<String> route = InitialRoutes.choose(true, routing, alive(Map.of("auth", "auth-1")));

        assertThat(route).isEqualTo(new InitialRoute.Join<>("auth-1", true));
    }

    @Test
    void authenticatedPlayersAreRefusedWhenNothingIsAlive() {
        InitialRoute<String> route = InitialRoutes.choose(true, routing, alive(Map.of()));

        assertThat(route).isEqualTo(new InitialRoute.Refuse<>("no-lobby-server"));
    }

    @Test
    void unauthenticatedPlayersStartOnTheAuthServer() {
        InitialRoute<String> route = InitialRoutes.choose(false, routing, alive(Map.of("lobby", "lobby-1", "auth", "auth-1")));

        assertThat(route).isEqualTo(new InitialRoute.Join<>("auth-1", false));
    }

    @Test
    void unauthenticatedPlayersNeverFallBackToTheLobby() {
        InitialRoute<String> route = InitialRoutes.choose(false, routing, alive(Map.of("lobby", "lobby-1")));

        assertThat(route).isEqualTo(new InitialRoute.Refuse<>("no-auth-server"));
    }

    private static Function<String, Optional<String>> alive(Map<String, String> serverByGroup) {
        return group -> Optional.ofNullable(serverByGroup.get(group));
    }
}

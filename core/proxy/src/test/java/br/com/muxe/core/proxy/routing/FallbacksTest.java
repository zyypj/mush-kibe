package br.com.muxe.core.proxy.routing;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.proxy.config.RoutingSettings;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FallbacksTest {

    private final RoutingSettings routing = new RoutingSettings("auth", "lobby");

    @Test
    void aPlayerWhoseLobbyClosesGoesToAnotherLobby() {
        Fallback<String> fallback = Fallbacks.choose(true, true, routing, others(Map.of("lobby", "lobby-2", "auth", "auth-1")));

        assertThat(fallback).isEqualTo(new Fallback.Redirect<>("lobby-2"));
    }

    @Test
    void withOnlyTheAuthServerLeftThePlayerIsDisconnected() {
        Fallback<String> fallback = Fallbacks.choose(true, true, routing, others(Map.of("auth", "auth-1")));

        assertThat(fallback).isEqualTo(new Fallback.Disconnect<>(Fallbacks.RESTARTING));
    }

    @Test
    void withEverythingDownThePlayerIsDisconnected() {
        assertThat(Fallbacks.choose(true, true, routing, others(Map.of()))).isEqualTo(new Fallback.Disconnect<>(Fallbacks.RESTARTING));
        assertThat(Fallbacks.choose(false, true, routing, others(Map.of()))).isEqualTo(new Fallback.Disconnect<>(Fallbacks.RESTARTING));
    }

    @Test
    void aLoggedInPlayerKickedFromAWorkingServerAlsoGoesToALobby() {
        assertThat(Fallbacks.choose(true, false, routing, others(Map.of("lobby", "lobby-1")))).isEqualTo(new Fallback.Redirect<>("lobby-1"));
    }

    @Test
    void aPlayerStillLoggingInMovesToAnotherAuthServerOnlyWhenTheirsCloses() {
        assertThat(Fallbacks.choose(false, true, routing, others(Map.of("auth", "auth-2", "lobby", "lobby-1")))).isEqualTo(new Fallback.Redirect<>("auth-2"));
        assertThat(Fallbacks.choose(false, false, routing, others(Map.of("auth", "auth-2")))).isEqualTo(new Fallback.Keep<>());
    }

    @Test
    void aServerIsClosingWhenItLeftTheListOrKickedWithTheShutdownReason() {
        assertThat(Fallbacks.isClosing(false, Optional.of("Senha incorreta"))).isTrue();
        assertThat(Fallbacks.isClosing(true, Optional.of("Server closed"))).isTrue();
        assertThat(Fallbacks.isClosing(true, Optional.empty())).isTrue();
        assertThat(Fallbacks.isClosing(true, Optional.of(" "))).isTrue();
        assertThat(Fallbacks.isClosing(true, Optional.of("Senha incorreta"))).isFalse();
    }

    private static Function<String, Optional<String>> others(Map<String, String> alive) {
        return group -> Optional.ofNullable(alive.get(group));
    }
}

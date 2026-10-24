package br.com.muxe.core.proxy.routing;

import br.com.muxe.core.proxy.config.RoutingSettings;
import java.util.Optional;
import java.util.function.Function;

public final class InitialRoutes {

    private InitialRoutes() {
    }

    public static <T> InitialRoute<T> choose(boolean authenticated, RoutingSettings routing, Function<String, Optional<T>> pick) {
        if (!authenticated) {
            return pick.apply(routing.authGroup())
                .<InitialRoute<T>>map(server -> new InitialRoute.Join<>(server, false))
                .orElseGet(() -> new InitialRoute.Refuse<>("no-auth-server"));
        }
        Optional<T> lobby = pick.apply(routing.lobbyGroup());
        if (lobby.isPresent()) {
            return new InitialRoute.Join<>(lobby.get(), false);
        }
        return pick.apply(routing.authGroup())
            .<InitialRoute<T>>map(server -> new InitialRoute.Join<>(server, true))
            .orElseGet(() -> new InitialRoute.Refuse<>("no-lobby-server"));
    }
}

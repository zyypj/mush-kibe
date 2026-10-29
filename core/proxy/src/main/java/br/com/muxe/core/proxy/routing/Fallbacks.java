package br.com.muxe.core.proxy.routing;

import br.com.muxe.core.proxy.config.RoutingSettings;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class Fallbacks {

    public static final String RESTARTING = "server-restarting";

    private static final Set<String> SHUTDOWN_REASONS = Set.of("server closed", "server is restarting", "server closed.");

    private Fallbacks() {
    }

    public static boolean isClosing(boolean stillAlive, Optional<String> reason) {
        return !stillAlive || reason.map(text -> text.isBlank() || SHUTDOWN_REASONS.contains(text.trim().toLowerCase(Locale.ROOT))).orElse(true);
    }

    public static <T> Fallback<T> choose(boolean authenticated, boolean closing, RoutingSettings routing, Function<String, Optional<T>> pickOther) {
        if (!authenticated && !closing) {
            return new Fallback.Keep<>();
        }
        String group = authenticated ? routing.lobbyGroup() : routing.authGroup();
        return pickOther.apply(group)
            .<Fallback<T>>map(Fallback.Redirect::new)
            .orElseGet(() -> new Fallback.Disconnect<>(RESTARTING));
    }
}

package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.proxy.login.LoginMode;
import br.com.muxe.core.proxy.login.LoginModeResolver;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PreLoginListener {

    private final LoginModeResolver resolver;
    private final Executor executor;
    private final ProxyMessages messages;
    private final MetricRegistry metrics;
    private final Logger logger;

    public PreLoginListener(LoginModeResolver resolver, Executor executor, ProxyMessages messages, MetricRegistry metrics, Logger logger) {
        this.resolver = resolver;
        this.executor = executor;
        this.messages = messages;
        this.metrics = metrics;
        this.logger = logger;
    }

    @Subscribe
    public EventTask onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        CompletableFuture<Void> decision = CompletableFuture
            .supplyAsync(() -> resolver.resolve(username), executor)
            .handle((mode, error) -> {
                if (!event.getResult().isAllowed()) {
                    return null;
                }
                if (error != null) {
                    logger.log(Level.WARNING, "Could not decide the login mode of " + username, error);
                    deny(event);
                    return null;
                }
                apply(event, mode);
                return null;
            });
        return EventTask.resumeWhenComplete(decision);
    }

    private void apply(PreLoginEvent event, LoginMode mode) {
        metrics.counter("muxe_auth_login_modes_total", Tags.of("mode", mode.name().toLowerCase(Locale.ROOT))).increment();
        switch (mode) {
            case PREMIUM -> event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            case CRACKED -> event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            case UNDECIDED -> deny(event);
        }
    }

    private void deny(PreLoginEvent event) {
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(messages.get("service-unavailable")));
    }
}

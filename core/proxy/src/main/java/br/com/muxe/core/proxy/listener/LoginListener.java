package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.common.account.application.command.RecordLoginCommand;
import br.com.muxe.core.common.account.domain.AccountProblems;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.proxy.auth.AuthenticationGate;
import br.com.muxe.core.proxy.session.LoginSessions;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;

public final class LoginListener {

    private final CommandBus commands;
    private final AuthenticationGate gate;
    private final LoginSessions sessions;
    private final ProxyMessages messages;
    private final MetricRegistry metrics;
    private final Logger logger;

    public LoginListener(CommandBus commands, AuthenticationGate gate, LoginSessions sessions, ProxyMessages messages, MetricRegistry metrics, Logger logger) {
        this.commands = commands;
        this.gate = gate;
        this.sessions = sessions;
        this.messages = messages;
        this.metrics = metrics;
        this.logger = logger;
    }

    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return null;
        }
        Player player = event.getPlayer();
        RecordLoginCommand command = new RecordLoginCommand(player.getUniqueId(), player.getUsername(), player.isOnlineMode());
        CompletableFuture<Void> recording = commands.dispatch(command)
            .thenAccept(result -> {
                if (result instanceof Result.Failure<?> failure) {
                    event.setResult(ResultedEvent.ComponentResult.denied(messageFor(failure.problem().code(), player.getUsername())));
                    return;
                }
                result.orElseThrow().nameReclaimedFrom().ifPresent(previous -> reportReclaimedName(player, previous));
                authenticateIfTrusted(player);
            })
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not record login of " + player.getUsername(), error);
                event.setResult(ResultedEvent.ComponentResult.denied(messages.get("service-unavailable")));
                return null;
            });
        return EventTask.resumeWhenComplete(recording);
    }

    private void authenticateIfTrusted(Player player) {
        String mode;
        if (player.isOnlineMode()) {
            gate.authenticate(player.getUniqueId());
            mode = "premium";
        } else if (canResumeSession(player)) {
            gate.authenticate(player.getUniqueId());
            mode = "session";
        } else {
            mode = "password";
        }
        metrics.counter("muxe_auth_logins_total", Tags.of("mode", mode)).increment();
    }

    private void reportReclaimedName(Player player, UUID previousHolder) {
        metrics.counter("muxe_accounts_names_reclaimed_total").increment();
        logger.info("Premium player " + player.getUniqueId() + " reclaimed the name " + player.getUsername() + " from account " + previousHolder);
    }

    private boolean canResumeSession(Player player) {
        try {
            return sessions.canResume(player.getUniqueId(), player.getRemoteAddress().getAddress());
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Login sessions unavailable, " + player.getUsername() + " must log in again: " + exception.getMessage());
            return false;
        }
    }

    private Component messageFor(String code, String username) {
        return switch (code) {
            case AccountProblems.NAME_CASE_CONFLICT -> messages.get("name-case-conflict", "username", username);
            case AccountProblems.INVALID_USERNAME -> messages.get("invalid-username", "username", username);
            default -> messages.get("service-unavailable");
        };
    }
}

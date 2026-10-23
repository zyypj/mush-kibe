package br.com.muxe.auth.flow;

import br.com.muxe.auth.credential.application.command.RegisterCredentialCommand;
import br.com.muxe.auth.credential.application.command.VerifyCredentialCommand;
import br.com.muxe.auth.credential.application.query.HasCredentialQuery;
import br.com.muxe.auth.credential.domain.AuthProblems;
import br.com.muxe.auth.session.AuthSessionRegistry;
import br.com.muxe.auth.session.AuthState;
import br.com.muxe.auth.session.LoginAttemptLimiter;
import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.auth.AuthenticationMethod;
import br.com.muxe.core.common.network.auth.PlayerAuthenticatedPacket;
import br.com.muxe.core.common.result.Result;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class AuthFlow {

    private final MuxeCore core;
    private final AuthSessionRegistry sessions;
    private final LoginAttemptLimiter limiter;
    private final MessageCatalog messages;
    private final Logger logger;

    public AuthFlow(MuxeCore core, AuthSessionRegistry sessions, LoginAttemptLimiter limiter, MessageCatalog messages, Logger logger) {
        this.core = core;
        this.sessions = sessions;
        this.limiter = limiter;
        this.messages = messages;
        this.logger = logger;
    }

    public void begin(Player player) {
        UUID playerId = player.getUniqueId();
        sessions.set(playerId, AuthState.LOADING);
        whenDone(player, core.queries().ask(new HasCredentialQuery(playerId)), (registered, error) -> {
            if (error != null) {
                fail(player, error);
                return;
            }
            sessions.set(playerId, registered ? AuthState.AWAITING_LOGIN : AuthState.AWAITING_REGISTER);
            player.sendMessage(messages.get(registered ? "prompt-login" : "prompt-register"));
        });
    }

    public void register(Player player, String password, String confirmation) {
        UUID playerId = player.getUniqueId();
        if (!sessions.transition(playerId, AuthState.AWAITING_REGISTER, AuthState.PROCESSING)) {
            player.sendMessage(messages.get(sessions.isAuthenticated(playerId) ? "already-authenticated" : "use-login"));
            return;
        }
        player.sendMessage(messages.get("processing"));
        RegisterCredentialCommand command = new RegisterCredentialCommand(playerId, player.getName(), password, confirmation);
        whenDone(player, core.commands().dispatch(command), (result, error) -> {
            if (error != null) {
                fail(player, error);
            } else if (result instanceof Result.Failure<Void> failure) {
                sessions.set(playerId, AuthState.AWAITING_REGISTER);
                player.sendMessage(messages.get(problemKey(failure.problem().code())));
            } else {
                complete(player, AuthenticationMethod.REGISTER);
            }
        });
    }

    public void login(Player player, String password) {
        UUID playerId = player.getUniqueId();
        if (limiter.isBlocked(playerId)) {
            player.kickPlayer(messages.get("too-many-attempts"));
            return;
        }
        if (!sessions.transition(playerId, AuthState.AWAITING_LOGIN, AuthState.PROCESSING)) {
            player.sendMessage(messages.get(sessions.isAuthenticated(playerId) ? "already-authenticated" : "use-register"));
            return;
        }
        player.sendMessage(messages.get("processing"));
        whenDone(player, core.commands().dispatch(new VerifyCredentialCommand(playerId, password)), (result, error) -> {
            if (error != null) {
                fail(player, error);
            } else if (result instanceof Result.Failure<Void> failure) {
                handleLoginFailure(player, failure.problem().code());
            } else {
                limiter.reset(playerId);
                complete(player, AuthenticationMethod.LOGIN);
            }
        });
    }

    public void end(UUID playerId) {
        sessions.remove(playerId);
    }

    public boolean isAuthenticated(UUID playerId) {
        return sessions.isAuthenticated(playerId);
    }

    private void handleLoginFailure(Player player, String code) {
        UUID playerId = player.getUniqueId();
        if (!AuthProblems.WRONG_PASSWORD.equals(code)) {
            sessions.set(playerId, AuthState.AWAITING_LOGIN);
            player.sendMessage(messages.get(problemKey(code)));
            return;
        }
        int remaining = limiter.recordFailure(playerId);
        if (remaining == 0) {
            player.kickPlayer(messages.get("too-many-attempts"));
            return;
        }
        sessions.set(playerId, AuthState.AWAITING_LOGIN);
        player.sendMessage(messages.get("wrong-password", "remaining", Integer.toString(remaining)));
    }

    private void complete(Player player, AuthenticationMethod method) {
        sessions.set(player.getUniqueId(), AuthState.AUTHENTICATED);
        player.sendMessage(messages.get(method == AuthenticationMethod.REGISTER ? "registered" : "logged-in"));
        PlayerAuthenticatedPacket packet = new PlayerAuthenticatedPacket(player.getUniqueId(), player.getName(), method);
        core.packets().publish(PacketTarget.proxy(), packet).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not notify the proxy about " + player.getName(), error);
            core.mainThread().execute(() -> player.kickPlayer(messages.get("service-unavailable")));
            return null;
        });
    }

    private void fail(Player player, Throwable error) {
        logger.log(Level.WARNING, "Authentication of " + player.getName() + " failed", error);
        player.kickPlayer(messages.get("service-unavailable"));
    }

    private <T> void whenDone(Player player, CompletableFuture<T> future, BiConsumer<T, Throwable> onMainThread) {
        future.whenComplete((value, error) -> core.mainThread().execute(() -> {
            if (player.isOnline()) {
                onMainThread.accept(value, error);
            }
        }));
    }

    private static String problemKey(String code) {
        return switch (code) {
            case AuthProblems.ALREADY_REGISTERED -> "use-login";
            case AuthProblems.NOT_REGISTERED -> "use-register";
            case AuthProblems.PASSWORDS_DO_NOT_MATCH -> "passwords-do-not-match";
            case AuthProblems.PASSWORD_TOO_SHORT -> "password-too-short";
            case AuthProblems.PASSWORD_TOO_LONG -> "password-too-long";
            case AuthProblems.PASSWORD_EQUALS_USERNAME -> "password-equals-username";
            default -> "service-unavailable";
        };
    }
}

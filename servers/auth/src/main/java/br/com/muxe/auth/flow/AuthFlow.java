package br.com.muxe.auth.flow;

import br.com.muxe.auth.challenge.ChallengeAnswer;
import br.com.muxe.auth.challenge.ChallengeRenderer;
import br.com.muxe.auth.challenge.ChallengeTracker;
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
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.network.auth.AuthenticationMethod;
import br.com.muxe.core.common.network.auth.PlayerAuthenticatedPacket;
import br.com.muxe.core.common.result.Result;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class AuthFlow {

    private final MuxeCore core;
    private final AuthSessionRegistry sessions;
    private final LoginAttemptLimiter limiter;
    private final ChallengeTracker challenges;
    private final ChallengeRenderer renderer;
    private final boolean challengeEnabled;
    private final AuthPrompts prompts;
    private final MessageCatalog messages;
    private final Logger logger;

    public AuthFlow(
        MuxeCore core,
        AuthSessionRegistry sessions,
        LoginAttemptLimiter limiter,
        ChallengeTracker challenges,
        ChallengeRenderer renderer,
        boolean challengeEnabled,
        AuthPrompts prompts,
        MessageCatalog messages,
        Logger logger
    ) {
        this.core = core;
        this.sessions = sessions;
        this.limiter = limiter;
        this.challenges = challenges;
        this.renderer = renderer;
        this.challengeEnabled = challengeEnabled;
        this.prompts = prompts;
        this.messages = messages;
        this.logger = logger;
    }

    public void begin(Player player, Location challengeAnchor) {
        UUID playerId = player.getUniqueId();
        sessions.set(playerId, AuthState.LOADING);
        whenDone(player, core.queries().ask(new HasCredentialQuery(playerId)), (registered, error) -> {
            if (error != null) {
                fail(player, error);
            } else if (challengeEnabled) {
                startChallenge(player, registered, challengeAnchor);
            } else {
                askCredentials(player, registered);
            }
        });
    }

    public void answerChallenge(Player player, String text) {
        UUID playerId = player.getUniqueId();
        ChallengeAnswer answer = challenges.answer(playerId, text);
        if (answer.outcome() != ChallengeAnswer.Outcome.NOT_IN_CHALLENGE) {
            core.metrics().counter("muxe_auth_challenges_total", Tags.of("outcome", answer.outcome().name().toLowerCase(Locale.ROOT))).increment();
        }
        switch (answer.outcome()) {
            case PASSED -> {
                renderer.untrack(playerId);
                player.sendMessage(messages.get("challenge-passed"));
                askCredentials(player, answer.registered());
            }
            case WRONG -> player.sendMessage(messages.get("challenge-wrong", "remaining", Integer.toString(answer.remainingAttempts())));
            case FAILED -> player.kickPlayer(messages.get("challenge-failed"));
            case NOT_IN_CHALLENGE -> {
            }
        }
    }

    public void register(Player player, String password, String confirmation) {
        UUID playerId = player.getUniqueId();
        if (!sessions.transition(playerId, AuthState.AWAITING_REGISTER, AuthState.PROCESSING)) {
            player.sendMessage(messages.get(blockedReason(playerId, "use-login")));
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
            player.sendMessage(messages.get(blockedReason(playerId, "use-register")));
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

    public boolean isInChallenge(UUID playerId) {
        return challenges.isChallenging(playerId);
    }

    public boolean isAuthenticated(UUID playerId) {
        return sessions.isAuthenticated(playerId);
    }

    public void end(UUID playerId) {
        sessions.remove(playerId);
        challenges.end(playerId);
        renderer.untrack(playerId);
    }

    private void startChallenge(Player player, boolean registered, Location anchor) {
        sessions.set(player.getUniqueId(), AuthState.CHALLENGE);
        challenges.start(player.getUniqueId(), registered);
        renderer.track(player.getUniqueId(), anchor);
        prompts.challenge(player);
    }

    private void askCredentials(Player player, boolean registered) {
        sessions.set(player.getUniqueId(), registered ? AuthState.AWAITING_LOGIN : AuthState.AWAITING_REGISTER);
        prompts.credentials(player, registered);
    }

    private String blockedReason(UUID playerId, String otherCommandKey) {
        AuthState state = sessions.state(playerId).orElse(AuthState.LOADING);
        return switch (state) {
            case AUTHENTICATED -> "already-authenticated";
            case CHALLENGE -> "complete-challenge-first";
            case LOADING, PROCESSING -> "processing";
            case AWAITING_LOGIN, AWAITING_REGISTER -> otherCommandKey;
        };
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
        prompts.authenticated(player, method == AuthenticationMethod.LOGIN);
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

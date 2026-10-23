package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.common.account.application.command.RecordLoginCommand;
import br.com.muxe.core.common.account.domain.AccountProblems;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.proxy.text.ProxyMessages;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;

public final class LoginListener {

    private final CommandBus commands;
    private final ProxyMessages messages;
    private final Logger logger;

    public LoginListener(CommandBus commands, ProxyMessages messages, Logger logger) {
        this.commands = commands;
        this.messages = messages;
        this.logger = logger;
    }

    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        CompletableFuture<Void> recording = commands.dispatch(new RecordLoginCommand(player.getUniqueId(), player.getUsername()))
            .thenAccept(result -> {
                if (result instanceof Result.Failure<?> failure) {
                    event.setResult(ResultedEvent.ComponentResult.denied(messageFor(failure.problem().code(), player.getUsername())));
                }
            })
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not record login of " + player.getUsername(), error);
                event.setResult(ResultedEvent.ComponentResult.denied(messages.get("service-unavailable")));
                return null;
            });
        return EventTask.resumeWhenComplete(recording);
    }

    private Component messageFor(String code, String username) {
        return switch (code) {
            case AccountProblems.NAME_CASE_CONFLICT -> messages.get("name-case-conflict", "username", username);
            case AccountProblems.INVALID_USERNAME -> messages.get("invalid-username", "username", username);
            default -> messages.get("service-unavailable");
        };
    }
}

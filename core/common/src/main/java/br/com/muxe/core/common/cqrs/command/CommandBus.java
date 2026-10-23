package br.com.muxe.core.common.cqrs.command;

import java.util.concurrent.CompletableFuture;

public interface CommandBus {

    <C extends Command<R>, R> void register(Class<C> type, CommandHandler<C, R> handler);

    <R> CompletableFuture<R> dispatch(Command<R> command);
}

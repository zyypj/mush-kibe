package br.com.muxe.core.common.cqrs.bus;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.cqrs.middleware.HandlerMiddleware;
import br.com.muxe.core.common.cqrs.middleware.MessageKind;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SimpleCommandBus implements CommandBus {

    private final MessageDispatcher dispatcher;

    public SimpleCommandBus(Executor executor, List<HandlerMiddleware> middlewares) {
        this.dispatcher = new MessageDispatcher(MessageKind.COMMAND, executor, middlewares);
    }

    @Override
    public <C extends Command<R>, R> void register(Class<C> type, CommandHandler<C, R> handler) {
        dispatcher.register(type, message -> handler.handle(type.cast(message)));
    }

    @Override
    public <R> CompletableFuture<R> dispatch(Command<R> command) {
        return dispatcher.dispatch(command);
    }
}

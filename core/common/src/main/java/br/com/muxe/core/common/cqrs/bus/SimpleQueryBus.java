package br.com.muxe.core.common.cqrs.bus;

import br.com.muxe.core.common.cqrs.middleware.HandlerMiddleware;
import br.com.muxe.core.common.cqrs.middleware.MessageKind;
import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.cqrs.query.QueryHandler;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SimpleQueryBus implements QueryBus {

    private final MessageDispatcher dispatcher;

    public SimpleQueryBus(Executor executor, List<HandlerMiddleware> middlewares) {
        this.dispatcher = new MessageDispatcher(MessageKind.QUERY, executor, middlewares);
    }

    @Override
    public <Q extends Query<R>, R> void register(Class<Q> type, QueryHandler<Q, R> handler) {
        dispatcher.register(type, message -> handler.handle(type.cast(message)));
    }

    @Override
    public <R> CompletableFuture<R> ask(Query<R> query) {
        return dispatcher.dispatch(query);
    }
}

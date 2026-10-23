package br.com.muxe.core.common.cqrs.query;

import java.util.concurrent.CompletableFuture;

public interface QueryBus {

    <Q extends Query<R>, R> void register(Class<Q> type, QueryHandler<Q, R> handler);

    <R> CompletableFuture<R> ask(Query<R> query);
}

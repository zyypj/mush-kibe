package br.com.muxe.core.common.cqrs.query;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

    R handle(Q query) throws Exception;
}

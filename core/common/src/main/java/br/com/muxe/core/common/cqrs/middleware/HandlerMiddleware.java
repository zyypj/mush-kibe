package br.com.muxe.core.common.cqrs.middleware;

@FunctionalInterface
public interface HandlerMiddleware {

    <R> R around(MessageKind kind, Object message, Invocation<R> next) throws Exception;
}

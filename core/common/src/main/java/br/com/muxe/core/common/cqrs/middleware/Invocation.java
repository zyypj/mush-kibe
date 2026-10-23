package br.com.muxe.core.common.cqrs.middleware;

@FunctionalInterface
public interface Invocation<R> {

    R proceed() throws Exception;
}

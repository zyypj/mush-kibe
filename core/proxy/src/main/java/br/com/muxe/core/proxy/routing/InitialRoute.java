package br.com.muxe.core.proxy.routing;

public sealed interface InitialRoute<T> {

    record Join<T>(T server, boolean lobbyUnavailable) implements InitialRoute<T> {
    }

    record Refuse<T>(String messageKey) implements InitialRoute<T> {
    }
}

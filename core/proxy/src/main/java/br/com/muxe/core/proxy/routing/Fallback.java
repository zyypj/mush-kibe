package br.com.muxe.core.proxy.routing;

public sealed interface Fallback<T> {

    record Redirect<T>(T server) implements Fallback<T> {
    }

    record Disconnect<T>(String messageKey) implements Fallback<T> {
    }

    record Keep<T>() implements Fallback<T> {
    }
}

package br.com.muxe.core.common.cqrs.bus;

public final class HandlerNotFoundException extends IllegalStateException {

    public HandlerNotFoundException(Class<?> messageType) {
        super("No handler registered for " + messageType.getName());
    }
}

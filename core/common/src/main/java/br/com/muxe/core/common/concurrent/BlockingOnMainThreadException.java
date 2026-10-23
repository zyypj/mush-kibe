package br.com.muxe.core.common.concurrent;

public final class BlockingOnMainThreadException extends IllegalStateException {

    public BlockingOnMainThreadException(String operation) {
        super("Blocking operation '" + operation + "' must not run on the main thread");
    }
}

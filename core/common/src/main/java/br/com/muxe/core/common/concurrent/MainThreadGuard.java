package br.com.muxe.core.common.concurrent;

@FunctionalInterface
public interface MainThreadGuard {

    boolean isMainThread();

    default void ensureNotMainThread(String operation) {
        if (isMainThread()) {
            throw new BlockingOnMainThreadException(operation);
        }
    }

    static MainThreadGuard none() {
        return () -> false;
    }
}

package br.com.muxe.core.common.messaging.transport;

public interface ConnectionListener {

    default void onConnected() {
    }

    default void onDisconnected(Throwable cause) {
    }
}

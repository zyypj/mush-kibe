package br.com.muxe.core.common.messaging.transport;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface MessageTransport extends AutoCloseable {

    CompletableFuture<Void> publish(String channel, byte[] message);

    void subscribe(Collection<String> channels, MessageListener listener);

    void addConnectionListener(ConnectionListener listener);

    boolean isConnected();

    @Override
    void close();
}

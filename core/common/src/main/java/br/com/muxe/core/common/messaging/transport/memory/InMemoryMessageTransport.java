package br.com.muxe.core.common.messaging.transport.memory;

import br.com.muxe.core.common.messaging.transport.ConnectionListener;
import br.com.muxe.core.common.messaging.transport.MessageListener;
import br.com.muxe.core.common.messaging.transport.MessageTransport;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryMessageTransport implements MessageTransport {

    private final InMemoryBroker broker;
    private final List<MessageListener> subscriptions = new CopyOnWriteArrayList<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private volatile boolean open = true;

    public InMemoryMessageTransport(InMemoryBroker broker) {
        this.broker = broker;
    }

    @Override
    public CompletableFuture<Void> publish(String channel, byte[] message) {
        if (!open) {
            return CompletableFuture.failedFuture(new IllegalStateException("Transport is closed"));
        }
        broker.publish(channel, message);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribe(Collection<String> channels, MessageListener listener) {
        MessageListener guarded = (channel, message) -> {
            if (open) {
                listener.onMessage(channel, message);
            }
        };
        subscriptions.add(guarded);
        channels.forEach(channel -> broker.subscribe(channel, guarded));
        connectionListeners.forEach(ConnectionListener::onConnected);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @Override
    public boolean isConnected() {
        return open;
    }

    @Override
    public void close() {
        open = false;
        subscriptions.forEach(broker::unsubscribe);
        subscriptions.clear();
    }
}

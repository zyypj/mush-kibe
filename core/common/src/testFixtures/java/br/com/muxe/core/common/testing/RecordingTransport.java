package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.messaging.transport.ConnectionListener;
import br.com.muxe.core.common.messaging.transport.MessageListener;
import br.com.muxe.core.common.messaging.transport.MessageTransport;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RecordingTransport implements MessageTransport {

    private final MessageTransport delegate;
    private final List<Published> published = new CopyOnWriteArrayList<>();

    public RecordingTransport(MessageTransport delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<Void> publish(String channel, byte[] message) {
        published.add(new Published(channel, message.clone()));
        return delegate.publish(channel, message);
    }

    @Override
    public void subscribe(Collection<String> channels, MessageListener listener) {
        delegate.subscribe(channels, listener);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        delegate.addConnectionListener(listener);
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public void close() {
        delegate.close();
    }

    public List<Published> published() {
        return List.copyOf(published);
    }

    public Published last() {
        return published.getLast();
    }

    public record Published(String channel, byte[] message) {
    }
}

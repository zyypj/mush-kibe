package br.com.muxe.core.common.messaging.transport.memory;

import br.com.muxe.core.common.messaging.transport.MessageListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryBroker {

    private final Map<String, List<MessageListener>> listeners = new ConcurrentHashMap<>();

    void subscribe(String channel, MessageListener listener) {
        listeners.computeIfAbsent(channel, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    void unsubscribe(MessageListener listener) {
        listeners.values().forEach(list -> list.remove(listener));
    }

    public int publish(String channel, byte[] message) {
        List<MessageListener> receivers = listeners.getOrDefault(channel, List.of());
        for (MessageListener receiver : receivers) {
            receiver.onMessage(channel, message.clone());
        }
        return receivers.size();
    }
}

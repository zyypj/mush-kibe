package br.com.muxe.core.common.messaging.bus.internal;

import br.com.muxe.core.common.messaging.bus.PacketHandler;
import br.com.muxe.core.common.messaging.bus.Subscription;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class HandlerRegistry {

    private final Map<String, List<PacketHandler<?>>> handlers = new ConcurrentHashMap<>();

    public <T extends Packet> Subscription add(PacketType<T> type, PacketHandler<T> handler) {
        List<PacketHandler<?>> list = handlers.computeIfAbsent(type.key(), key -> new CopyOnWriteArrayList<>());
        list.add(handler);
        return () -> list.remove(handler);
    }

    public List<PacketHandler<?>> handlersFor(String typeKey) {
        return handlers.getOrDefault(typeKey, List.of());
    }
}

package br.com.muxe.core.common.messaging.packet;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketRegistry {

    private final Map<String, PacketType<?>> byKey = new ConcurrentHashMap<>();
    private final Map<Class<?>, PacketType<?>> byClass = new ConcurrentHashMap<>();

    public synchronized <T extends Packet> PacketRegistry register(PacketType<T> type) {
        PacketType<?> existingKey = byKey.get(type.key());
        if (existingKey != null && !existingKey.equals(type)) {
            throw new IllegalStateException("Packet " + type.key() + " is already registered");
        }
        PacketType<?> existingClass = byClass.get(type.javaType());
        if (existingClass != null && !existingClass.equals(type)) {
            throw new IllegalStateException(type.javaType().getName() + " is already registered as " + existingClass.key());
        }
        byKey.put(type.key(), type);
        byClass.put(type.javaType(), type);
        return this;
    }

    public Optional<PacketType<?>> find(String id, int version) {
        return Optional.ofNullable(byKey.get(id + "@" + version));
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet> PacketType<T> typeOf(Class<T> javaType) {
        PacketType<?> type = byClass.get(javaType);
        if (type == null) {
            throw new UnknownPacketException(javaType.getName() + " is not registered in the PacketRegistry");
        }
        return (PacketType<T>) type;
    }

    public int size() {
        return byKey.size();
    }
}

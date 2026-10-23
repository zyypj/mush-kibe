package br.com.muxe.core.common.messaging.packet;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import java.util.Objects;
import java.util.regex.Pattern;

public record PacketType<T extends Packet>(String id, int version, Class<T> javaType, PacketCodec<T> codec, DeliveryPolicy policy) {

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+");

    public PacketType {
        Objects.requireNonNull(javaType, "javaType");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(policy, "policy");
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Packet id must look like 'area.name', got: " + id);
        }
        if (version < 1) {
            throw new IllegalArgumentException("Packet version must be at least 1");
        }
    }

    public static <T extends Packet> PacketType<T> of(String id, Class<T> javaType, PacketCodec<T> codec) {
        return new PacketType<>(id, 1, javaType, codec, DeliveryPolicy.standard());
    }

    public static <T extends Packet> PacketType<T> of(String id, Class<T> javaType, PacketCodec<T> codec, DeliveryPolicy policy) {
        return new PacketType<>(id, 1, javaType, codec, policy);
    }

    public String key() {
        return id + "@" + version;
    }
}

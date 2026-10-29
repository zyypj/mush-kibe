package br.com.muxe.core.common.network.permission;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PermissionsChangedPacket(Optional<UUID> user) implements Packet {

    private static final String EVERYTHING = "*";

    public static final PacketType<PermissionsChangedPacket> TYPE = PacketType.of(
        "permission.changed",
        PermissionsChangedPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeString(packet.user().map(UUID::toString).orElse(EVERYTHING)),
            in -> {
                String value = in.readString();
                return new PermissionsChangedPacket(value.equals(EVERYTHING) ? Optional.empty() : Optional.of(UUID.fromString(value)));
            }
        )
    );

    public PermissionsChangedPacket {
        Objects.requireNonNull(user, "user");
    }

    public static PermissionsChangedPacket everything() {
        return new PermissionsChangedPacket(Optional.empty());
    }

    public static PermissionsChangedPacket of(UUID user) {
        return new PermissionsChangedPacket(Optional.of(user));
    }
}

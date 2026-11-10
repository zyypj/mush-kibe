package br.com.muxe.core.common.network.language;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record LanguageChangedPacket(UUID playerId, Language language) implements Packet {

    public static final PacketType<LanguageChangedPacket> TYPE = PacketType.of(
        "language.changed",
        LanguageChangedPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeUuid(packet.playerId()).writeString(packet.language().tag()),
            in -> new LanguageChangedPacket(in.readUuid(), Language.parse(in.readString()))
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(30))
    );

    public LanguageChangedPacket {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(language, "language");
    }
}

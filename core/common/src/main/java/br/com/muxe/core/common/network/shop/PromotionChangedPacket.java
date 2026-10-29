package br.com.muxe.core.common.network.shop;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.util.Objects;

public record PromotionChangedPacket(String productId) implements Packet {

    public static final PacketType<PromotionChangedPacket> TYPE = PacketType.of(
        "shop.promotion_changed",
        PromotionChangedPacket.class,
        PacketCodec.of(
            (packet, out) -> out.writeString(packet.productId()),
            in -> new PromotionChangedPacket(in.readString())
        )
    );

    public PromotionChangedPacket {
        Objects.requireNonNull(productId, "productId");
    }
}

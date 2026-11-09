package br.com.muxe.core.common.network.customroom;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;

public record CreateCustomRoomReply(boolean accepted, String code) implements Packet {

    public static final PacketType<CreateCustomRoomReply> TYPE = PacketType.of(
        "custom_room.create_reply",
        CreateCustomRoomReply.class,
        PacketCodec.of(
            (packet, out) -> out.writeBoolean(packet.accepted()).writeString(packet.code()),
            in -> new CreateCustomRoomReply(in.readBoolean(), in.readString())
        ),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5))
    );

    public static CreateCustomRoomReply created(String code) {
        return new CreateCustomRoomReply(true, code);
    }

    public static CreateCustomRoomReply refused() {
        return new CreateCustomRoomReply(false, "");
    }
}

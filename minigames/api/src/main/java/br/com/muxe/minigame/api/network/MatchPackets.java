package br.com.muxe.minigame.api.network;

import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;

public final class MatchPackets {

    private final PacketType<RoomsPacket> rooms;
    private final PacketType<JoinRoomRequest> joinRequest;
    private final PacketType<JoinRoomReply> joinReply;

    public MatchPackets(String game) {
        this.rooms = PacketType.of(game + ".rooms", RoomsPacket.class, RoomsPacket.CODEC, DeliveryPolicy.volatileState(Duration.ofSeconds(10)));
        this.joinRequest = new PacketType<>(game + ".join_request", JoinRoomRequest.VERSION, JoinRoomRequest.class, JoinRoomRequest.CODEC,
            DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5)));
        this.joinReply = PacketType.of(game + ".join_reply", JoinRoomReply.class, JoinRoomReply.CODEC,
            DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(5)));
    }

    public PacketType<RoomsPacket> rooms() {
        return rooms;
    }

    public PacketType<JoinRoomRequest> joinRequest() {
        return joinRequest;
    }

    public PacketType<JoinRoomReply> joinReply() {
        return joinReply;
    }

    public void registerAll(PacketRegistry registry) {
        registry.register(rooms).register(joinRequest).register(joinReply);
    }
}

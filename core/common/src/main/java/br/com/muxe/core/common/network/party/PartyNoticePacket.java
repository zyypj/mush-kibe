package br.com.muxe.core.common.network.party;

import br.com.muxe.core.common.messaging.codec.PacketCodec;
import br.com.muxe.core.common.messaging.packet.DeliveryPolicy;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.party.domain.PartyNotice;
import java.time.Duration;

public record PartyNoticePacket(PartyNotice notice) implements Packet {

    public static final PacketType<PartyNoticePacket> TYPE = PacketType.of(
        "party.notice",
        PartyNoticePacket.class,
        PacketCodec.of((packet, out) -> PartyCodecs.writeNotice(out, packet.notice()), in -> new PartyNoticePacket(PartyCodecs.readNotice(in))),
        DeliveryPolicy.standard().withMaxAge(Duration.ofSeconds(10))
    );
}

package br.com.muxe.core.common.network.party;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.network.NetworkPackets;
import br.com.muxe.core.common.network.player.OnlinePlayersPacket;
import br.com.muxe.core.common.network.player.PlayerDisplayPacket;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartyPacketsTest {

    private static final UUID LEADER = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();

    @Test
    void everyPartyPacketSurvivesTheWire() {
        PartySnapshot party = new PartySnapshot(UUID.randomUUID(), LEADER, true, List.of(
            new PartySnapshot.Member(LEADER, "tadeu", true), new PartySnapshot.Member(MEMBER, "tadeudobw", false)));
        PartyNotice notice = PartyNotice.of(List.of(LEADER, MEMBER), "party-invite", "player", "&5tadeu").clicking("/party aceitar tadeu");

        assertRoundTrip(PartyActionPacket.TYPE, new PartyActionPacket(LEADER, "tadeu", PartyAction.INVITE, List.of("a", "b"), false));
        assertRoundTrip(PartyNoticePacket.TYPE, new PartyNoticePacket(notice));
        assertRoundTrip(PartyNoticePacket.TYPE, new PartyNoticePacket(PartyNotice.raw(List.of(MEMBER), "§d[PARTY] oi")));
        assertRoundTrip(PartyUpdatedPacket.TYPE, new PartyUpdatedPacket(party));
        assertRoundTrip(PartyRemovedPacket.TYPE, new PartyRemovedPacket(party.id()));
        assertRoundTrip(PartySyncRequest.TYPE, new PartySyncRequest("lobby-1"));
        assertRoundTrip(PartySyncResponse.TYPE, new PartySyncResponse(List.of(party)));
        assertRoundTrip(PartyGatherRequest.TYPE, new PartyGatherRequest(LEADER, List.of(MEMBER)));
        assertRoundTrip(PartyGatherResponse.TYPE, new PartyGatherResponse(List.of(MEMBER)));
        assertRoundTrip(PlayerDisplayPacket.TYPE, new PlayerDisplayPacket(LEADER, "&5tadeu"));
        assertRoundTrip(OnlinePlayersPacket.TYPE, new OnlinePlayersPacket(List.of("tadeu", "tadeudobw")));
    }

    @Test
    void everyServerKnowsThePartyPackets() {
        PacketRegistry registry = NetworkPackets.registerAll(new PacketRegistry());

        List.of(PartyActionPacket.TYPE, PartyNoticePacket.TYPE, PartyUpdatedPacket.TYPE, PartyRemovedPacket.TYPE, PartySyncRequest.TYPE,
                PartySyncResponse.TYPE, PartyGatherRequest.TYPE, PartyGatherResponse.TYPE, PlayerDisplayPacket.TYPE, OnlinePlayersPacket.TYPE)
            .forEach(type -> assertThat(registry.find(type.id(), 1)).isPresent());
    }

    private static <T extends Packet> void assertRoundTrip(PacketType<T> type, T packet) {
        PacketWriter out = new PacketWriter();
        type.codec().encode(packet, out);
        PacketReader in = new PacketReader(out.toByteArray());
        T decoded = type.codec().decode(in);
        in.ensureFullyRead();
        assertThat(decoded).isEqualTo(packet);
    }
}

package br.com.muxe.core.common.network.placement;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.network.NetworkPackets;
import br.com.muxe.core.common.network.shop.PromotionChangedPacket;
import org.junit.jupiter.api.Test;

class PlacementPacketsTest {

    @Test
    void placementChangesNameTheGroup() {
        assertThat(roundTrip(PlacementsChangedPacket.TYPE, new PlacementsChangedPacket("lobby"))).isEqualTo(new PlacementsChangedPacket("lobby"));
    }

    @Test
    void promotionChangesNameTheProduct() {
        assertThat(roundTrip(PromotionChangedPacket.TYPE, new PromotionChangedPacket("enderlore"))).isEqualTo(new PromotionChangedPacket("enderlore"));
    }

    @Test
    void bothPacketsAreKnownToEveryServer() {
        PacketRegistry registry = NetworkPackets.registerAll(new PacketRegistry());

        assertThat(registry.find(PlacementsChangedPacket.TYPE.id(), 1)).isPresent();
        assertThat(registry.find(PromotionChangedPacket.TYPE.id(), 1)).isPresent();
    }

    private static <T extends Packet> T roundTrip(PacketType<T> type, T packet) {
        PacketWriter out = new PacketWriter();
        type.codec().encode(packet, out);
        PacketReader in = new PacketReader(out.toByteArray());
        T decoded = type.codec().decode(in);
        in.ensureFullyRead();
        return decoded;
    }
}

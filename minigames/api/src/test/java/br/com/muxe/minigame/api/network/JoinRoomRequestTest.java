package br.com.muxe.minigame.api.network;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JoinRoomRequestTest {

    @Test
    void theLeaderCarriesThePartyCompanionsInANewVersion() {
        JoinRoomRequest request = new JoinRoomRequest(UUID.randomUUID(), "solo", "farol", "", List.of(UUID.randomUUID(), UUID.randomUUID()));
        PacketWriter out = new PacketWriter();
        JoinRoomRequest.CODEC.encode(request, out);
        PacketReader in = new PacketReader(out.toByteArray());

        JoinRoomRequest decoded = JoinRoomRequest.CODEC.decode(in);
        in.ensureFullyRead();

        assertThat(decoded).isEqualTo(request);
        assertThat(decoded.seats()).isEqualTo(3);
        assertThat(new MatchPackets("bedwars").joinRequest().version()).isEqualTo(2);
        assertThat(new JoinRoomRequest(UUID.randomUUID(), "solo", "", "").seats()).isEqualTo(1);
    }
}

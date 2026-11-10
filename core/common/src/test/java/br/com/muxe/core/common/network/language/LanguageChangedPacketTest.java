package br.com.muxe.core.common.network.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.network.NetworkPackets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LanguageChangedPacketTest {

    @Test
    void theLanguageSurvivesTheWire() {
        LanguageChangedPacket packet = new LanguageChangedPacket(UUID.randomUUID(), Language.ENGLISH);
        PacketWriter out = new PacketWriter();
        LanguageChangedPacket.TYPE.codec().encode(packet, out);
        PacketReader in = new PacketReader(out.toByteArray());

        LanguageChangedPacket decoded = LanguageChangedPacket.TYPE.codec().decode(in);

        in.ensureFullyRead();
        assertThat(decoded).isEqualTo(packet);
    }

    @Test
    void everyServerKnowsThePacket() {
        PacketRegistry registry = NetworkPackets.registerAll(new PacketRegistry());

        assertThat(registry.find(LanguageChangedPacket.TYPE.id(), 1)).isPresent();
    }
}

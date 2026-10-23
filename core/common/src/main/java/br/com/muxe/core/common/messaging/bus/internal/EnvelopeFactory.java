package br.com.muxe.core.common.messaging.bus.internal;

import br.com.muxe.core.common.messaging.envelope.EnvelopeKind;
import br.com.muxe.core.common.messaging.envelope.PacketEnvelope;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Clock;
import java.util.UUID;

public final class EnvelopeFactory {

    private final String origin;
    private final Clock clock;

    public EnvelopeFactory(String origin, Clock clock) {
        this.origin = origin;
        this.clock = clock;
    }

    public PacketEnvelope create(PacketType<?> type, Packet packet, EnvelopeKind kind, UUID correlationId) {
        byte[] payload = Payloads.encode(type, packet);
        return new PacketEnvelope(UUID.randomUUID(), kind, type.id(), type.version(), origin, correlationId, clock.millis(), payload);
    }
}

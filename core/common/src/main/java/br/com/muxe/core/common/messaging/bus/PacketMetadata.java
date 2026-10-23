package br.com.muxe.core.common.messaging.bus;

import br.com.muxe.core.common.messaging.envelope.EnvelopeKind;
import java.time.Instant;
import java.util.UUID;

public record PacketMetadata(
    UUID packetId,
    EnvelopeKind kind,
    String origin,
    String channel,
    UUID correlationId,
    Instant sentAt,
    Instant receivedAt
) {

    public boolean isRequest() {
        return kind == EnvelopeKind.REQUEST;
    }
}

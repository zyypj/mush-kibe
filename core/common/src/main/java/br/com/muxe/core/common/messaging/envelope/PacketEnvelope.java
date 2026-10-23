package br.com.muxe.core.common.messaging.envelope;

import java.util.Objects;
import java.util.UUID;

public record PacketEnvelope(
    UUID packetId,
    EnvelopeKind kind,
    String typeId,
    int typeVersion,
    String origin,
    UUID correlationId,
    long sentAtMillis,
    byte[] payload
) {

    public PacketEnvelope {
        Objects.requireNonNull(packetId, "packetId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(payload, "payload");
        if (kind != EnvelopeKind.MESSAGE && correlationId == null) {
            throw new IllegalArgumentException(kind + " envelopes need a correlation id");
        }
    }

    public String typeKey() {
        return typeId + "@" + typeVersion;
    }
}

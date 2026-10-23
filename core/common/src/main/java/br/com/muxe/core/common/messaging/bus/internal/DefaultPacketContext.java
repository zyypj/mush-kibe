package br.com.muxe.core.common.messaging.bus.internal;

import br.com.muxe.core.common.messaging.bus.PacketContext;
import br.com.muxe.core.common.messaging.bus.PacketMetadata;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.messaging.envelope.EnvelopeKind;
import br.com.muxe.core.common.messaging.packet.Packet;
import java.util.concurrent.CompletableFuture;

record DefaultPacketContext<T extends Packet>(T packet, PacketMetadata metadata, PacketPublisher publisher) implements PacketContext<T> {

    @Override
    public CompletableFuture<Void> reply(Packet response) {
        if (!metadata.isRequest()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Only requests can be replied to"));
        }
        return publisher.send(PacketTarget.server(metadata.origin()), response, EnvelopeKind.RESPONSE, metadata.correlationId());
    }
}

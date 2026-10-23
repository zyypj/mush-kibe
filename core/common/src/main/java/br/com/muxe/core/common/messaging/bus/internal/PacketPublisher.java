package br.com.muxe.core.common.messaging.bus.internal;

import br.com.muxe.core.common.messaging.channel.ChannelNames;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.messaging.codec.PacketCodecException;
import br.com.muxe.core.common.messaging.envelope.EnvelopeCodec;
import br.com.muxe.core.common.messaging.envelope.EnvelopeKind;
import br.com.muxe.core.common.messaging.envelope.EnvelopeRejectedException;
import br.com.muxe.core.common.messaging.metrics.PacketMetrics;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.messaging.packet.UnknownPacketException;
import br.com.muxe.core.common.messaging.transport.MessageTransport;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PacketPublisher {

    private final PacketRegistry registry;
    private final EnvelopeFactory envelopes;
    private final EnvelopeCodec codec;
    private final MessageTransport transport;
    private final ChannelNames channels;
    private final PacketMetrics metrics;

    public PacketPublisher(PacketRegistry registry, EnvelopeFactory envelopes, EnvelopeCodec codec, MessageTransport transport, ChannelNames channels, PacketMetrics metrics) {
        this.registry = registry;
        this.envelopes = envelopes;
        this.codec = codec;
        this.transport = transport;
        this.channels = channels;
        this.metrics = metrics;
    }

    public CompletableFuture<Void> send(PacketTarget target, Packet packet, EnvelopeKind kind, UUID correlationId) {
        PacketType<?> type;
        try {
            type = registry.typeOf(packet.getClass());
        } catch (UnknownPacketException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        byte[] message;
        try {
            message = codec.encode(envelopes.create(type, packet, kind, correlationId));
        } catch (EnvelopeRejectedException exception) {
            metrics.rejected(exception.reason());
            metrics.publishFailed(type.id());
            return CompletableFuture.failedFuture(exception);
        } catch (PacketCodecException exception) {
            metrics.publishFailed(type.id());
            return CompletableFuture.failedFuture(exception);
        }
        long start = System.nanoTime();
        int size = message.length;
        return transport.publish(channels.of(target), message).whenComplete((ignored, error) -> {
            if (error == null) {
                metrics.published(type.id(), size, System.nanoTime() - start);
            } else {
                metrics.publishFailed(type.id());
            }
        });
    }
}

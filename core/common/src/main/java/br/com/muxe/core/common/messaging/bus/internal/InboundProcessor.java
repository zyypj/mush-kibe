package br.com.muxe.core.common.messaging.bus.internal;

import br.com.muxe.core.common.messaging.bus.PacketHandler;
import br.com.muxe.core.common.messaging.bus.PacketMetadata;
import br.com.muxe.core.common.messaging.codec.PacketCodecException;
import br.com.muxe.core.common.messaging.envelope.EnvelopeCodec;
import br.com.muxe.core.common.messaging.envelope.EnvelopeKind;
import br.com.muxe.core.common.messaging.envelope.EnvelopeRejectedException;
import br.com.muxe.core.common.messaging.envelope.PacketEnvelope;
import br.com.muxe.core.common.messaging.envelope.RejectionReason;
import br.com.muxe.core.common.messaging.idempotency.ClaimResult;
import br.com.muxe.core.common.messaging.idempotency.IdempotencyGuard;
import br.com.muxe.core.common.messaging.metrics.PacketMetrics;
import br.com.muxe.core.common.messaging.packet.IdempotentPacket;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.messaging.request.PendingRequests;
import br.com.muxe.core.common.messaging.transport.MessageListener;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class InboundProcessor implements MessageListener {

    private final String selfId;
    private final PacketRegistry registry;
    private final EnvelopeCodec codec;
    private final HandlerRegistry handlers;
    private final PendingRequests pendingRequests;
    private final IdempotencyGuard idempotency;
    private final PacketPublisher publisher;
    private final PacketMetrics metrics;
    private final Clock clock;
    private final Executor executor;
    private final Logger logger;

    public InboundProcessor(
        String selfId,
        PacketRegistry registry,
        EnvelopeCodec codec,
        HandlerRegistry handlers,
        PendingRequests pendingRequests,
        IdempotencyGuard idempotency,
        PacketPublisher publisher,
        PacketMetrics metrics,
        Clock clock,
        Executor executor,
        Logger logger
    ) {
        this.selfId = selfId;
        this.registry = registry;
        this.codec = codec;
        this.handlers = handlers;
        this.pendingRequests = pendingRequests;
        this.idempotency = idempotency;
        this.publisher = publisher;
        this.metrics = metrics;
        this.clock = clock;
        this.executor = executor;
        this.logger = logger;
    }

    @Override
    public void onMessage(String channel, byte[] message) {
        try {
            executor.execute(() -> process(channel, message));
        } catch (RejectedExecutionException exception) {
            logger.log(Level.FINE, "Dropping packet received during shutdown");
        }
    }

    public void process(String channel, byte[] message) {
        PacketEnvelope envelope;
        try {
            envelope = codec.decode(message);
        } catch (EnvelopeRejectedException exception) {
            reject(exception.reason(), exception.getMessage());
            return;
        }
        if (envelope.origin().equals(selfId)) {
            return;
        }
        Optional<PacketType<?>> found = registry.find(envelope.typeId(), envelope.typeVersion());
        if (found.isEmpty()) {
            reject(RejectionReason.UNKNOWN_TYPE, envelope.typeKey() + " from " + envelope.origin());
            return;
        }
        PacketType<?> type = found.get();
        Instant receivedAt = clock.instant();
        metrics.received(type.id(), message.length, receivedAt.toEpochMilli() - envelope.sentAtMillis());

        if (receivedAt.toEpochMilli() - envelope.sentAtMillis() > type.policy().maxAge().toMillis()) {
            reject(RejectionReason.EXPIRED, envelope.typeKey() + " from " + envelope.origin());
            return;
        }
        if (envelope.kind() == EnvelopeKind.RESPONSE) {
            if (!pendingRequests.complete(envelope.correlationId(), envelope)) {
                reject(RejectionReason.LATE_RESPONSE, envelope.typeKey() + " from " + envelope.origin());
            }
            return;
        }
        List<PacketHandler<?>> typeHandlers = handlers.handlersFor(type.key());
        if (typeHandlers.isEmpty()) {
            metrics.unhandled(type.id());
            return;
        }
        dispatch(type, envelope, channel, receivedAt, typeHandlers);
    }

    private void dispatch(PacketType<?> type, PacketEnvelope envelope, String channel, Instant receivedAt, List<PacketHandler<?>> typeHandlers) {
        Packet packet;
        try {
            packet = Payloads.decode(type, envelope.payload());
        } catch (PacketCodecException exception) {
            reject(RejectionReason.MALFORMED_PAYLOAD, envelope.typeKey() + ": " + exception.getMessage());
            return;
        }
        String key = packet instanceof IdempotentPacket idempotent ? idempotent.idempotencyKey() : envelope.packetId().toString();
        ClaimResult claim = idempotency.claim(type.id(), key, type.policy());
        if (claim == ClaimResult.DUPLICATE) {
            metrics.duplicate(type.id());
            return;
        }
        if (claim == ClaimResult.UNAVAILABLE) {
            reject(RejectionReason.IDEMPOTENCY_UNAVAILABLE, envelope.typeKey());
            return;
        }
        PacketMetadata metadata = new PacketMetadata(
            envelope.packetId(),
            envelope.kind(),
            envelope.origin(),
            channel,
            envelope.correlationId(),
            Instant.ofEpochMilli(envelope.sentAtMillis()),
            receivedAt
        );
        DefaultPacketContext<Packet> context = new DefaultPacketContext<>(packet, metadata, publisher);
        for (PacketHandler<?> handler : typeHandlers) {
            invoke(type, handler, context);
        }
    }

    @SuppressWarnings("unchecked")
    private void invoke(PacketType<?> type, PacketHandler<?> handler, DefaultPacketContext<Packet> context) {
        long start = System.nanoTime();
        try {
            ((PacketHandler<Packet>) handler).handle(context);
        } catch (Exception exception) {
            metrics.handlerFailed(type.id());
            logger.log(Level.WARNING, "Handler for " + type.key() + " failed", exception);
        } finally {
            metrics.handled(type.id(), System.nanoTime() - start);
        }
    }

    private void reject(RejectionReason reason, String detail) {
        metrics.rejected(reason);
        logger.log(Level.FINE, "Packet rejected (" + reason.tag() + "): " + detail);
    }
}

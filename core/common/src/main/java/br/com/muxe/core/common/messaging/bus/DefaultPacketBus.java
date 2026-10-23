package br.com.muxe.core.common.messaging.bus;

import br.com.muxe.core.common.messaging.bus.internal.EnvelopeFactory;
import br.com.muxe.core.common.messaging.bus.internal.HandlerRegistry;
import br.com.muxe.core.common.messaging.bus.internal.InboundProcessor;
import br.com.muxe.core.common.messaging.bus.internal.PacketPublisher;
import br.com.muxe.core.common.messaging.bus.internal.Payloads;
import br.com.muxe.core.common.messaging.channel.ChannelNames;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.messaging.envelope.EnvelopeCodec;
import br.com.muxe.core.common.messaging.envelope.EnvelopeKind;
import br.com.muxe.core.common.messaging.envelope.PacketEnvelope;
import br.com.muxe.core.common.messaging.idempotency.IdempotencyGuard;
import br.com.muxe.core.common.messaging.metrics.PacketMetrics;
import br.com.muxe.core.common.messaging.metrics.RequestOutcome;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.packet.PacketType;
import br.com.muxe.core.common.messaging.packet.UnknownPacketException;
import br.com.muxe.core.common.messaging.request.PendingRequests;
import br.com.muxe.core.common.messaging.request.RequestTimeoutException;
import br.com.muxe.core.common.messaging.transport.MessageTransport;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.server.ServerIdentity;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class DefaultPacketBus implements PacketBus {

    private final ServerIdentity identity;
    private final PacketRegistry registry;
    private final MessageTransport transport;
    private final ChannelNames channels;
    private final HandlerRegistry handlers = new HandlerRegistry();
    private final PendingRequests pendingRequests;
    private final PacketPublisher publisher;
    private final InboundProcessor inbound;
    private final PacketMetrics metrics;
    private final AtomicBoolean started = new AtomicBoolean();

    private DefaultPacketBus(Builder builder) {
        this.identity = Objects.requireNonNull(builder.identity, "identity");
        this.registry = Objects.requireNonNull(builder.registry, "registry");
        this.transport = Objects.requireNonNull(builder.transport, "transport");
        this.channels = builder.channels;
        EnvelopeCodec codec = Objects.requireNonNull(builder.codec, "codec");
        IdempotencyGuard idempotency = Objects.requireNonNull(builder.idempotency, "idempotency");
        MetricRegistry metricRegistry = Objects.requireNonNull(builder.metrics, "metrics");
        Executor executor = Objects.requireNonNull(builder.executor, "executor");
        ScheduledExecutorService scheduler = Objects.requireNonNull(builder.scheduler, "scheduler");
        Logger logger = Objects.requireNonNull(builder.logger, "logger");

        this.metrics = new PacketMetrics(metricRegistry);
        this.pendingRequests = new PendingRequests(scheduler);
        this.publisher = new PacketPublisher(registry, new EnvelopeFactory(identity.id(), builder.clock), codec, transport, channels, metrics);
        this.inbound = new InboundProcessor(identity.id(), registry, codec, handlers, pendingRequests, idempotency, publisher, metrics, builder.clock, executor, logger);
        metricRegistry.gauge("muxe_requests_pending", pendingRequests::size);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<Void> publish(PacketTarget target, Packet packet) {
        return publisher.send(target, packet, EnvelopeKind.MESSAGE, null);
    }

    @Override
    public <R extends Packet> CompletableFuture<R> request(PacketTarget target, Packet request, Class<R> responseType, Duration timeout) {
        String typeId;
        try {
            typeId = registry.typeOf(request.getClass()).id();
        } catch (UnknownPacketException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        UUID correlationId = UUID.randomUUID();
        long start = System.nanoTime();
        CompletableFuture<PacketEnvelope> response = pendingRequests.register(correlationId, typeId, timeout);
        publisher.send(target, request, EnvelopeKind.REQUEST, correlationId).whenComplete((ignored, error) -> {
            if (error != null) {
                pendingRequests.fail(correlationId, error);
            }
        });
        return response
            .thenApply(envelope -> decodeResponse(envelope, responseType))
            .whenComplete((value, error) -> metrics.requestCompleted(typeId, outcomeOf(error), System.nanoTime() - start));
    }

    @Override
    public <T extends Packet> Subscription subscribe(PacketType<T> type, PacketHandler<T> handler) {
        registry.register(type);
        return handlers.add(type, handler);
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            transport.subscribe(channels.subscriptionsFor(identity), inbound);
        }
    }

    @Override
    public void close() {
        pendingRequests.failAll(new IllegalStateException("Packet bus closed"));
    }

    private <R extends Packet> R decodeResponse(PacketEnvelope envelope, Class<R> responseType) {
        PacketType<?> type = registry.find(envelope.typeId(), envelope.typeVersion())
            .orElseThrow(() -> new UnknownPacketException("Unknown response type " + envelope.typeKey()));
        return responseType.cast(Payloads.decode(type, envelope.payload()));
    }

    private static RequestOutcome outcomeOf(Throwable error) {
        if (error == null) {
            return RequestOutcome.SUCCESS;
        }
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause instanceof RequestTimeoutException ? RequestOutcome.TIMEOUT : RequestOutcome.FAILURE;
    }

    public static final class Builder {

        private ServerIdentity identity;
        private PacketRegistry registry;
        private MessageTransport transport;
        private ChannelNames channels = new ChannelNames();
        private EnvelopeCodec codec;
        private IdempotencyGuard idempotency;
        private MetricRegistry metrics;
        private Executor executor;
        private ScheduledExecutorService scheduler;
        private Clock clock = Clock.systemUTC();
        private Logger logger;

        private Builder() {
        }

        public Builder identity(ServerIdentity value) {
            this.identity = value;
            return this;
        }

        public Builder registry(PacketRegistry value) {
            this.registry = value;
            return this;
        }

        public Builder transport(MessageTransport value) {
            this.transport = value;
            return this;
        }

        public Builder channels(ChannelNames value) {
            this.channels = value;
            return this;
        }

        public Builder codec(EnvelopeCodec value) {
            this.codec = value;
            return this;
        }

        public Builder idempotency(IdempotencyGuard value) {
            this.idempotency = value;
            return this;
        }

        public Builder metrics(MetricRegistry value) {
            this.metrics = value;
            return this;
        }

        public Builder executor(Executor value) {
            this.executor = value;
            return this;
        }

        public Builder scheduler(ScheduledExecutorService value) {
            this.scheduler = value;
            return this;
        }

        public Builder clock(Clock value) {
            this.clock = value;
            return this;
        }

        public Builder logger(Logger value) {
            this.logger = value;
            return this;
        }

        public DefaultPacketBus build() {
            return new DefaultPacketBus(this);
        }
    }
}

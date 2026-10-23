package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.messaging.bus.DefaultPacketBus;
import br.com.muxe.core.common.messaging.envelope.EnvelopeCodec;
import br.com.muxe.core.common.messaging.idempotency.IdempotencyGuard;
import br.com.muxe.core.common.messaging.idempotency.IdempotencyStore;
import br.com.muxe.core.common.messaging.idempotency.memory.InMemoryIdempotencyStore;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.security.HmacEnvelopeSigner;
import br.com.muxe.core.common.messaging.transport.memory.InMemoryBroker;
import br.com.muxe.core.common.messaging.transport.memory.InMemoryMessageTransport;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.network.NetworkPackets;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public final class TestNetwork implements AutoCloseable {

    public static final String SECRET = "test-secret-with-at-least-32-bytes!!";

    private final InMemoryBroker broker = new InMemoryBroker();
    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final IdempotencyStore sharedStore = new InMemoryIdempotencyStore(Clock.systemUTC(), 10_000);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<Node> nodes = new ArrayList<>();

    public Node node(String id, String group, ServerKind kind) {
        return node(new ServerIdentity(id, group, kind), SECRET);
    }

    public Node node(ServerIdentity identity, String secret) {
        RecordingTransport transport = new RecordingTransport(new InMemoryMessageTransport(broker));
        SimpleMetricRegistry metrics = new SimpleMetricRegistry();
        PacketRegistry registry = NetworkPackets.registerAll(new PacketRegistry());
        DefaultPacketBus bus = DefaultPacketBus.builder()
            .identity(identity)
            .registry(registry)
            .transport(transport)
            .codec(new EnvelopeCodec(HmacEnvelopeSigner.of(secret)))
            .idempotency(new IdempotencyGuard(identity.id(), new InMemoryIdempotencyStore(clock, 10_000), sharedStore, Logger.getLogger("test")))
            .metrics(metrics)
            .executor(Runnable::run)
            .scheduler(scheduler)
            .clock(clock)
            .logger(Logger.getLogger("test." + identity.id()))
            .build();
        Node node = new Node(identity, bus, transport, metrics, registry);
        nodes.add(node);
        return node;
    }

    public InMemoryBroker broker() {
        return broker;
    }

    public MutableClock clock() {
        return clock;
    }

    @Override
    public void close() {
        nodes.forEach(node -> node.bus().close());
        scheduler.shutdownNow();
    }

    public record Node(ServerIdentity identity, DefaultPacketBus bus, RecordingTransport transport, SimpleMetricRegistry metrics, PacketRegistry registry) {

        public Node start() {
            bus.start();
            return this;
        }

        public long counter(String name) {
            return metrics.snapshot().counterTotal(name);
        }
    }
}

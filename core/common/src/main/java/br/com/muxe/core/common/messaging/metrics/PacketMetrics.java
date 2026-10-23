package br.com.muxe.core.common.messaging.metrics;

import br.com.muxe.core.common.messaging.envelope.RejectionReason;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.metrics.instrument.Counter;
import br.com.muxe.core.common.metrics.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class PacketMetrics {

    private final MetricRegistry registry;
    private final Map<String, TypeMeters> meters = new ConcurrentHashMap<>();
    private final Map<RejectionReason, Counter> rejections = new ConcurrentHashMap<>();

    public PacketMetrics(MetricRegistry registry) {
        this.registry = registry;
    }

    public void published(String type, int bytes, long nanos) {
        TypeMeters meter = meters(type);
        meter.published.increment();
        meter.publishedBytes.add(bytes);
        meter.publishTime.record(nanos);
    }

    public void publishFailed(String type) {
        meters(type).publishFailures.increment();
    }

    public void received(String type, int bytes, long latencyMillis) {
        TypeMeters meter = meters(type);
        meter.received.increment();
        meter.receivedBytes.add(bytes);
        meter.latency.record(TimeUnit.MILLISECONDS.toNanos(Math.max(0, latencyMillis)));
    }

    public void handled(String type, long nanos) {
        meters(type).handleTime.record(nanos);
    }

    public void handlerFailed(String type) {
        meters(type).handlerFailures.increment();
    }

    public void duplicate(String type) {
        meters(type).duplicates.increment();
    }

    public void unhandled(String type) {
        meters(type).unhandled.increment();
    }

    public void rejected(RejectionReason reason) {
        rejections.computeIfAbsent(reason, key -> registry.counter("muxe_packets_rejected_total", Tags.of("reason", key.tag()))).increment();
    }

    public void requestCompleted(String type, RequestOutcome outcome, long nanos) {
        Tags tags = Tags.of("type", type, "outcome", outcome.tag());
        registry.counter("muxe_requests_total", tags).increment();
        registry.timer("muxe_request_seconds", tags).record(nanos);
    }

    private TypeMeters meters(String type) {
        return meters.computeIfAbsent(type, key -> new TypeMeters(registry, Tags.of("type", key)));
    }

    private static final class TypeMeters {

        private final Counter published;
        private final Counter publishedBytes;
        private final Counter publishFailures;
        private final Timer publishTime;
        private final Counter received;
        private final Counter receivedBytes;
        private final Timer latency;
        private final Timer handleTime;
        private final Counter handlerFailures;
        private final Counter duplicates;
        private final Counter unhandled;

        private TypeMeters(MetricRegistry registry, Tags tags) {
            this.published = registry.counter("muxe_packets_published_total", tags);
            this.publishedBytes = registry.counter("muxe_packets_published_bytes_total", tags);
            this.publishFailures = registry.counter("muxe_packets_publish_failures_total", tags);
            this.publishTime = registry.timer("muxe_packets_publish_seconds", tags);
            this.received = registry.counter("muxe_packets_received_total", tags);
            this.receivedBytes = registry.counter("muxe_packets_received_bytes_total", tags);
            this.latency = registry.timer("muxe_packets_latency_seconds", tags);
            this.handleTime = registry.timer("muxe_packets_handle_seconds", tags);
            this.handlerFailures = registry.counter("muxe_packets_handler_failures_total", tags);
            this.duplicates = registry.counter("muxe_packets_duplicates_total", tags);
            this.unhandled = registry.counter("muxe_packets_unhandled_total", tags);
        }
    }
}

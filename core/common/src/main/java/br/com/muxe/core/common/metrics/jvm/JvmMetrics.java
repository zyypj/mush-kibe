package br.com.muxe.core.common.metrics.jvm;

import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

public final class JvmMetrics {

    private JvmMetrics() {
    }

    public static void register(MetricRegistry registry) {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        registry.gauge("jvm_memory_heap_used_bytes", () -> memory.getHeapMemoryUsage().getUsed());
        registry.gauge("jvm_memory_heap_committed_bytes", () -> memory.getHeapMemoryUsage().getCommitted());
        registry.gauge("jvm_memory_heap_max_bytes", () -> memory.getHeapMemoryUsage().getMax());
        registry.gauge("jvm_memory_non_heap_used_bytes", () -> memory.getNonHeapMemoryUsage().getUsed());
        registry.gauge("jvm_threads_live", threads::getThreadCount);
        registry.gauge("jvm_threads_daemon", threads::getDaemonThreadCount);
        registry.gauge("jvm_uptime_seconds", () -> runtime.getUptime() / 1000.0);
        registry.gauge("jvm_cpu_available_processors", () -> Runtime.getRuntime().availableProcessors());

        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            Tags tags = Tags.of("gc", collector.getName());
            registry.gauge("jvm_gc_collections", tags, collector::getCollectionCount);
            registry.gauge("jvm_gc_collection_seconds", tags, () -> collector.getCollectionTime() / 1000.0);
        }
    }
}

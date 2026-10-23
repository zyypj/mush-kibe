package br.com.muxe.core.common.metrics.simple;

import br.com.muxe.core.common.metrics.MetricId;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.metrics.instrument.Counter;
import br.com.muxe.core.common.metrics.instrument.Timer;
import br.com.muxe.core.common.metrics.snapshot.MetricsSnapshot;
import br.com.muxe.core.common.metrics.snapshot.TimerSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

public final class SimpleMetricRegistry implements MetricRegistry {

    private final Map<MetricId, Counter> counters = new ConcurrentHashMap<>();
    private final Map<MetricId, Timer> timers = new ConcurrentHashMap<>();
    private final Map<MetricId, DoubleSupplier> gauges = new ConcurrentHashMap<>();

    @Override
    public Counter counter(String name, Tags tags) {
        return counters.computeIfAbsent(new MetricId(name, tags), id -> new Counter());
    }

    @Override
    public Timer timer(String name, Tags tags) {
        return timers.computeIfAbsent(new MetricId(name, tags), id -> new Timer());
    }

    @Override
    public void gauge(String name, Tags tags, DoubleSupplier value) {
        gauges.put(new MetricId(name, tags), value);
    }

    @Override
    public MetricsSnapshot snapshot() {
        Map<MetricId, Long> counterValues = new HashMap<>();
        counters.forEach((id, counter) -> counterValues.put(id, counter.count()));
        Map<MetricId, TimerSnapshot> timerValues = new HashMap<>();
        timers.forEach((id, timer) -> timerValues.put(id, timer.snapshot()));
        Map<MetricId, Double> gaugeValues = new HashMap<>();
        gauges.forEach((id, supplier) -> gaugeValues.put(id, readGauge(supplier)));
        return new MetricsSnapshot(counterValues, timerValues, gaugeValues);
    }

    private static double readGauge(DoubleSupplier supplier) {
        try {
            return supplier.getAsDouble();
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }
}

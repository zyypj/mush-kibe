package br.com.muxe.core.common.metrics.snapshot;

import br.com.muxe.core.common.metrics.MetricId;
import java.util.Map;

public record MetricsSnapshot(Map<MetricId, Long> counters, Map<MetricId, TimerSnapshot> timers, Map<MetricId, Double> gauges) {

    public MetricsSnapshot {
        counters = Map.copyOf(counters);
        timers = Map.copyOf(timers);
        gauges = Map.copyOf(gauges);
    }

    public long counterTotal(String name) {
        return counters.entrySet().stream()
            .filter(entry -> entry.getKey().name().equals(name))
            .mapToLong(Map.Entry::getValue)
            .sum();
    }

    public long timerCount(String name) {
        return timers.entrySet().stream()
            .filter(entry -> entry.getKey().name().equals(name))
            .mapToLong(entry -> entry.getValue().count())
            .sum();
    }

    public double gaugeTotal(String name) {
        return gauges.entrySet().stream()
            .filter(entry -> entry.getKey().name().equals(name))
            .mapToDouble(Map.Entry::getValue)
            .sum();
    }
}

package br.com.muxe.core.common.metrics;

import br.com.muxe.core.common.metrics.instrument.Counter;
import br.com.muxe.core.common.metrics.instrument.Timer;
import br.com.muxe.core.common.metrics.snapshot.MetricsSnapshot;
import java.util.function.DoubleSupplier;

public interface MetricRegistry {

    Counter counter(String name, Tags tags);

    Timer timer(String name, Tags tags);

    void gauge(String name, Tags tags, DoubleSupplier value);

    MetricsSnapshot snapshot();

    default Counter counter(String name) {
        return counter(name, Tags.EMPTY);
    }

    default Timer timer(String name) {
        return timer(name, Tags.EMPTY);
    }

    default void gauge(String name, DoubleSupplier value) {
        gauge(name, Tags.EMPTY, value);
    }
}

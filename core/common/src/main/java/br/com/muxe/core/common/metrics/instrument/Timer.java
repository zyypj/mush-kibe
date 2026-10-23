package br.com.muxe.core.common.metrics.instrument;

import br.com.muxe.core.common.metrics.snapshot.TimerSnapshot;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public final class Timer {

    public static final double[] DEFAULT_BUCKETS_SECONDS = {
        0.0005, 0.001, 0.0025, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10
    };

    private final double[] boundsSeconds;
    private final long[] boundsNanos;
    private final LongAdder[] buckets;
    private final LongAdder count = new LongAdder();
    private final LongAdder totalNanos = new LongAdder();
    private final LongAccumulator maxNanos = new LongAccumulator(Long::max, 0);

    public Timer() {
        this(DEFAULT_BUCKETS_SECONDS);
    }

    public Timer(double[] boundsSeconds) {
        this.boundsSeconds = boundsSeconds.clone();
        this.boundsNanos = new long[boundsSeconds.length];
        this.buckets = new LongAdder[boundsSeconds.length];
        for (int index = 0; index < boundsSeconds.length; index++) {
            if (index > 0 && boundsSeconds[index] <= boundsSeconds[index - 1]) {
                throw new IllegalArgumentException("Bucket bounds must be increasing");
            }
            boundsNanos[index] = (long) (boundsSeconds[index] * TimeUnit.SECONDS.toNanos(1));
            buckets[index] = new LongAdder();
        }
    }

    public void record(long nanos) {
        long value = Math.max(0, nanos);
        count.increment();
        totalNanos.add(value);
        maxNanos.accumulate(value);
        for (int index = 0; index < boundsNanos.length; index++) {
            if (value <= boundsNanos[index]) {
                buckets[index].increment();
                return;
            }
        }
    }

    public void record(Duration duration) {
        record(duration.toNanos());
    }

    public <T> T time(Supplier<T> work) {
        long start = System.nanoTime();
        try {
            return work.get();
        } finally {
            record(System.nanoTime() - start);
        }
    }

    public TimerSnapshot snapshot() {
        long[] cumulative = new long[buckets.length];
        long running = 0;
        for (int index = 0; index < buckets.length; index++) {
            running += buckets[index].sum();
            cumulative[index] = running;
        }
        return new TimerSnapshot(count.sum(), totalNanos.sum(), maxNanos.get(), boundsSeconds.clone(), cumulative);
    }
}

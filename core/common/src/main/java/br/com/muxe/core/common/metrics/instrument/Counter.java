package br.com.muxe.core.common.metrics.instrument;

import java.util.concurrent.atomic.LongAdder;

public final class Counter {

    private final LongAdder adder = new LongAdder();

    public void increment() {
        adder.increment();
    }

    public void add(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Counters only go up, got: " + amount);
        }
        adder.add(amount);
    }

    public long count() {
        return adder.sum();
    }
}

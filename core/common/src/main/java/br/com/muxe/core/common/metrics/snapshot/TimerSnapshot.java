package br.com.muxe.core.common.metrics.snapshot;

import java.util.concurrent.TimeUnit;

public record TimerSnapshot(long count, long totalNanos, long maxNanos, double[] boundsSeconds, long[] cumulativeCounts) {

    public double totalSeconds() {
        return totalNanos / (double) TimeUnit.SECONDS.toNanos(1);
    }

    public double maxSeconds() {
        return maxNanos / (double) TimeUnit.SECONDS.toNanos(1);
    }

    public double meanSeconds() {
        return count == 0 ? 0 : totalSeconds() / count;
    }
}

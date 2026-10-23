package br.com.muxe.core.bukkit.monitor;

import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;

public final class TickMonitor implements Runnable {

    private static final int WINDOW = 100;
    private static final int SAMPLE_EVERY_TICKS = 20;
    private static final double MAX_TPS = 20.0;

    private final BukkitServerLoad load;
    private final Timer tickInterval;
    private final long[] intervals = new long[WINDOW];
    private int cursor;
    private int filled;
    private long ticks;
    private long lastTickNanos;
    private volatile double tps = MAX_TPS;

    public TickMonitor(BukkitServerLoad load, MetricRegistry metrics) {
        this.load = load;
        this.tickInterval = metrics.timer("muxe_server_tick_interval_seconds");
        metrics.gauge("muxe_server_tps", () -> tps);
        metrics.gauge("muxe_server_players_online", load::onlinePlayers);
        metrics.gauge("muxe_server_players_max", load::maxPlayers);
    }

    @Override
    public void run() {
        long now = System.nanoTime();
        if (lastTickNanos != 0) {
            long interval = now - lastTickNanos;
            tickInterval.record(interval);
            intervals[cursor] = interval;
            cursor = (cursor + 1) % WINDOW;
            filled = Math.min(filled + 1, WINDOW);
        }
        lastTickNanos = now;
        if (++ticks % SAMPLE_EVERY_TICKS == 0) {
            tps = computeTps();
            load.sample(Bukkit.getOnlinePlayers().size());
        }
    }

    public double tps() {
        return tps;
    }

    private double computeTps() {
        if (filled == 0) {
            return MAX_TPS;
        }
        long total = 0;
        for (int index = 0; index < filled; index++) {
            total += intervals[index];
        }
        double averageNanos = total / (double) filled;
        return Math.min(MAX_TPS, TimeUnit.SECONDS.toNanos(1) / averageNanos);
    }
}

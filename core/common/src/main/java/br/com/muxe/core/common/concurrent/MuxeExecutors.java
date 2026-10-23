package br.com.muxe.core.common.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class MuxeExecutors implements AutoCloseable {

    private final ExecutorService io;
    private final ScheduledExecutorService scheduler;

    private MuxeExecutors(ExecutorService io, ScheduledExecutorService scheduler) {
        this.io = io;
        this.scheduler = scheduler;
    }

    public static MuxeExecutors create() {
        ExecutorService io = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("muxe-io-", 0).factory());
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, Thread.ofPlatform().name("muxe-scheduler-", 0).daemon(true).factory());
        scheduler.setRemoveOnCancelPolicy(true);
        return new MuxeExecutors(io, scheduler);
    }

    public ExecutorService io() {
        return io;
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        io.shutdown();
        try {
            if (!io.awaitTermination(5, TimeUnit.SECONDS)) {
                io.shutdownNow();
            }
        } catch (InterruptedException exception) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

package br.com.muxe.core.common.messaging.request;

import br.com.muxe.core.common.messaging.envelope.PacketEnvelope;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class PendingRequests {

    private final ScheduledExecutorService scheduler;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public PendingRequests(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public CompletableFuture<PacketEnvelope> register(UUID correlationId, String type, Duration timeout) {
        CompletableFuture<PacketEnvelope> future = new CompletableFuture<>();
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            Pending expired = pending.remove(correlationId);
            if (expired != null) {
                expired.future().completeExceptionally(new RequestTimeoutException(type, timeout));
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        pending.put(correlationId, new Pending(future, timeoutTask));
        return future;
    }

    public boolean complete(UUID correlationId, PacketEnvelope response) {
        Pending request = pending.remove(correlationId);
        if (request == null) {
            return false;
        }
        request.timeoutTask().cancel(false);
        return request.future().complete(response);
    }

    public void fail(UUID correlationId, Throwable cause) {
        Pending request = pending.remove(correlationId);
        if (request != null) {
            request.timeoutTask().cancel(false);
            request.future().completeExceptionally(cause);
        }
    }

    public void failAll(Throwable cause) {
        for (UUID correlationId : pending.keySet()) {
            fail(correlationId, cause);
        }
    }

    public int size() {
        return pending.size();
    }

    private record Pending(CompletableFuture<PacketEnvelope> future, ScheduledFuture<?> timeoutTask) {
    }
}

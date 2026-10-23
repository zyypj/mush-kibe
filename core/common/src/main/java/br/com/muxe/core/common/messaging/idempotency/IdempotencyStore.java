package br.com.muxe.core.common.messaging.idempotency;

import java.time.Duration;

public interface IdempotencyStore {

    boolean tryClaim(String key, Duration ttl);
}

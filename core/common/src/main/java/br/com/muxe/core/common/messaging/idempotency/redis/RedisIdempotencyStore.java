package br.com.muxe.core.common.messaging.idempotency.redis;

import br.com.muxe.core.common.messaging.idempotency.IdempotencyStore;
import br.com.muxe.core.common.redis.RedisConnector;
import java.time.Duration;
import redis.clients.jedis.params.SetParams;

public final class RedisIdempotencyStore implements IdempotencyStore {

    public static final String DEFAULT_PREFIX = "muxe:idempotency:";

    private final RedisConnector connector;
    private final String prefix;

    public RedisIdempotencyStore(RedisConnector connector) {
        this(connector, DEFAULT_PREFIX);
    }

    public RedisIdempotencyStore(RedisConnector connector, String prefix) {
        this.connector = connector;
        this.prefix = prefix;
    }

    @Override
    public boolean tryClaim(String key, Duration ttl) {
        long millis = Math.max(1, ttl.toMillis());
        String reply = connector.commands().set(prefix + key, "1", SetParams.setParams().nx().px(millis));
        return "OK".equals(reply);
    }
}

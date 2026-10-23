package br.com.muxe.core.common.store.redis;

import br.com.muxe.core.common.redis.RedisConnector;
import br.com.muxe.core.common.store.ExpiringStore;
import java.time.Duration;
import java.util.Optional;
import redis.clients.jedis.params.SetParams;

public final class RedisExpiringStore implements ExpiringStore {

    public static final String DEFAULT_PREFIX = "muxe:";

    private final RedisConnector connector;
    private final String prefix;

    public RedisExpiringStore(RedisConnector connector) {
        this(connector, DEFAULT_PREFIX);
    }

    public RedisExpiringStore(RedisConnector connector, String prefix) {
        this.connector = connector;
        this.prefix = prefix;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(connector.commands().get(prefix + key));
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Every stored value needs a positive time to live");
        }
        connector.commands().set(prefix + key, value, SetParams.setParams().px(ttl.toMillis()));
    }

    @Override
    public boolean delete(String key) {
        return connector.commands().del(prefix + key) > 0;
    }
}

package br.com.muxe.core.common.redis;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.UnifiedJedis;

public final class RedisConnector implements AutoCloseable {

    private final HostAndPort address;
    private final JedisClientConfig clientConfig;
    private final RedisClient client;

    private RedisConnector(HostAndPort address, JedisClientConfig clientConfig, RedisClient client) {
        this.address = address;
        this.clientConfig = clientConfig;
        this.client = client;
    }

    public static RedisConnector connect(RedisConfig config, String clientName) {
        HostAndPort address = new HostAndPort(config.host(), config.port());
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
            .clientName(clientName)
            .database(config.database())
            .timeoutMillis((int) config.timeout().toMillis());
        if (config.ssl()) {
            builder.sslOptions(SslOptions.defaults());
        }
        if (config.hasUsername()) {
            builder.user(config.username());
        }
        if (config.hasPassword()) {
            builder.password(config.password());
        }
        JedisClientConfig clientConfig = builder.build();

        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(config.poolSize());
        poolConfig.setMaxIdle(config.poolSize());
        poolConfig.setMinIdle(1);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setJmxEnabled(false);

        RedisClient client = RedisClient.builder()
            .hostAndPort(address)
            .clientConfig(clientConfig)
            .poolConfig(poolConfig)
            .build();
        RedisConnector connector = new RedisConnector(address, clientConfig, client);
        try {
            connector.ping();
        } catch (RuntimeException exception) {
            connector.close();
            throw exception;
        }
        return connector;
    }

    public UnifiedJedis commands() {
        return client;
    }

    public Jedis openDedicatedConnection() {
        return new Jedis(address, clientConfig);
    }

    public void ping() {
        client.ping();
    }

    @Override
    public void close() {
        client.close();
    }
}

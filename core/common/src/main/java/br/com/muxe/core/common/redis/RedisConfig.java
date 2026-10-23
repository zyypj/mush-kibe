package br.com.muxe.core.common.redis;

import java.time.Duration;
import java.util.Objects;

public record RedisConfig(
    String host,
    int port,
    String username,
    String password,
    int database,
    boolean ssl,
    int poolSize,
    Duration timeout
) {

    public RedisConfig {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(timeout, "timeout");
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Invalid Redis port: " + port);
        }
        if (poolSize < 2) {
            throw new IllegalArgumentException("Redis pool size must be at least 2");
        }
    }

    public boolean hasUsername() {
        return username != null && !username.isBlank();
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "RedisConfig[" + host + ":" + port + "/" + database + ", ssl=" + ssl + "]";
    }
}

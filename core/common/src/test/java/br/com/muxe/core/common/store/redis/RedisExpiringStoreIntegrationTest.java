package br.com.muxe.core.common.store.redis;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.redis.RedisConfig;
import br.com.muxe.core.common.redis.RedisConnector;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RedisExpiringStoreIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private static RedisConnector connector;

    @BeforeAll
    static void connect() {
        connector = RedisConnector.connect(new RedisConfig(REDIS.getHost(), REDIS.getMappedPort(6379), "", "", 0, false, 2, Duration.ofSeconds(2)), "store-test");
    }

    @AfterAll
    static void close() {
        connector.close();
    }

    @Test
    void storesReadsAndDeletesValuesWithAnExpiration() {
        RedisExpiringStore store = new RedisExpiringStore(connector);

        store.put("test:key", "value", Duration.ofMinutes(1));

        assertThat(store.get("test:key")).contains("value");
        assertThat(connector.commands().pttl("muxe:test:key")).isPositive();
        assertThat(store.delete("test:key")).isTrue();
        assertThat(store.get("test:key")).isEmpty();
    }

    @Test
    void reportsMissingKeys() {
        RedisExpiringStore store = new RedisExpiringStore(connector);

        assertThat(store.get("test:missing")).isEmpty();
        assertThat(store.delete("test:missing")).isFalse();
    }
}

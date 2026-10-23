package br.com.muxe.core.common.messaging.transport.redis;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.concurrent.MuxeExecutors;
import br.com.muxe.core.common.messaging.bus.DefaultPacketBus;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.messaging.envelope.EnvelopeCodec;
import br.com.muxe.core.common.messaging.idempotency.IdempotencyGuard;
import br.com.muxe.core.common.messaging.idempotency.memory.InMemoryIdempotencyStore;
import br.com.muxe.core.common.messaging.idempotency.redis.RedisIdempotencyStore;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.security.HmacEnvelopeSigner;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.network.NetworkPackets;
import br.com.muxe.core.common.network.player.PlayerLocateRequest;
import br.com.muxe.core.common.network.player.PlayerLocateResponse;
import br.com.muxe.core.common.redis.RedisConfig;
import br.com.muxe.core.common.redis.RedisConnector;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RedisMessagingIntegrationTest {

    private static final String SECRET = "integration-secret-with-32-bytes-ok!";

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private final List<AutoCloseable> resources = new ArrayList<>();
    private final List<RedisMessageTransport> transports = new ArrayList<>();

    @AfterEach
    void close() throws Exception {
        for (int index = resources.size() - 1; index >= 0; index--) {
            resources.get(index).close();
        }
    }

    @Test
    void requestAndResponseTravelThroughRedis() throws Exception {
        DefaultPacketBus proxy = bus(new ServerIdentity("proxy-1", "proxy", ServerKind.PROXY));
        DefaultPacketBus lobby = bus(new ServerIdentity("lobby-1", "lobby", ServerKind.LOBBY));
        proxy.subscribe(PlayerLocateRequest.TYPE, context ->
            context.reply(new PlayerLocateResponse(true, context.packet().username(), "lobby-1")));
        proxy.start();
        lobby.start();
        awaitSubscriptions();

        PlayerLocateResponse response = lobby
            .request(PacketTarget.proxy(), new PlayerLocateRequest("Steve"), PlayerLocateResponse.class, Duration.ofSeconds(3))
            .get(5, TimeUnit.SECONDS);

        assertThat(response.serverId()).isEqualTo("lobby-1");
    }

    @Test
    void redisClaimsAreGrantedOnce() {
        RedisIdempotencyStore store = new RedisIdempotencyStore(connector());

        assertThat(store.tryClaim("integration:key", Duration.ofSeconds(30))).isTrue();
        assertThat(store.tryClaim("integration:key", Duration.ofSeconds(30))).isFalse();
    }

    private DefaultPacketBus bus(ServerIdentity identity) {
        MuxeExecutors executors = track(MuxeExecutors.create());
        RedisConnector connector = connector();
        SimpleMetricRegistry metrics = new SimpleMetricRegistry();
        Logger logger = Logger.getLogger("it." + identity.id());
        RedisMessageTransport transport = track(new RedisMessageTransport(connector, executors.io(), executors.scheduler(), metrics, logger));
        transports.add(transport);
        return track(DefaultPacketBus.builder()
            .identity(identity)
            .registry(NetworkPackets.registerAll(new PacketRegistry()))
            .transport(transport)
            .codec(new EnvelopeCodec(HmacEnvelopeSigner.of(SECRET)))
            .idempotency(new IdempotencyGuard(identity.id(), new InMemoryIdempotencyStore(Clock.systemUTC(), 1_000), new RedisIdempotencyStore(connector), logger))
            .metrics(metrics)
            .executor(executors.io())
            .scheduler(executors.scheduler())
            .logger(logger)
            .build());
    }

    private RedisConnector connector() {
        RedisConfig config = new RedisConfig(REDIS.getHost(), REDIS.getMappedPort(6379), "", "", 0, false, 4, Duration.ofSeconds(2));
        return track(RedisConnector.connect(config, "integration-test"));
    }

    private void awaitSubscriptions() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (transports.stream().anyMatch(transport -> !transport.isConnected())) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Redis subscriptions were not established");
            }
            Thread.sleep(20);
        }
    }

    private <T extends AutoCloseable> T track(T resource) {
        resources.add(resource);
        return resource;
    }
}

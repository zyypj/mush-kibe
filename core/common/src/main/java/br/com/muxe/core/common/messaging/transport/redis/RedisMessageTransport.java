package br.com.muxe.core.common.messaging.transport.redis;

import br.com.muxe.core.common.messaging.transport.ConnectionListener;
import br.com.muxe.core.common.messaging.transport.MessageListener;
import br.com.muxe.core.common.messaging.transport.MessageTransport;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.instrument.Counter;
import br.com.muxe.core.common.redis.RedisConnector;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

public final class RedisMessageTransport implements MessageTransport {

    private static final Duration MIN_BACKOFF = Duration.ofMillis(250);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(10);
    private static final Duration HEALTH_CHECK_INTERVAL = Duration.ofSeconds(10);

    private final RedisConnector connector;
    private final Executor publishExecutor;
    private final ScheduledExecutorService scheduler;
    private final Logger logger;
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final Counter reconnects;

    private volatile boolean running;
    private volatile boolean connected;
    private volatile Subscriber subscriber;
    private volatile Jedis subscriberConnection;
    private Thread subscriberThread;
    private ScheduledFuture<?> healthCheck;

    public RedisMessageTransport(RedisConnector connector, Executor publishExecutor, ScheduledExecutorService scheduler, MetricRegistry metrics, Logger logger) {
        this.connector = connector;
        this.publishExecutor = publishExecutor;
        this.scheduler = scheduler;
        this.logger = logger;
        this.reconnects = metrics.counter("muxe_transport_reconnects_total");
        metrics.gauge("muxe_transport_connected", () -> connected ? 1 : 0);
    }

    @Override
    public CompletableFuture<Void> publish(String channel, byte[] message) {
        byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
        return CompletableFuture.runAsync(() -> connector.commands().publish(channelBytes, message), publishExecutor);
    }

    @Override
    public synchronized void subscribe(Collection<String> channels, MessageListener listener) {
        if (running) {
            throw new IllegalStateException("Transport is already subscribed");
        }
        running = true;
        byte[][] channelBytes = channels.stream().map(channel -> channel.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new);
        subscriberThread = Thread.ofPlatform()
            .name("muxe-redis-subscriber")
            .daemon(true)
            .start(() -> subscribeLoop(channelBytes, listener));
        healthCheck = scheduler.scheduleAtFixedRate(this::checkHealth, HEALTH_CHECK_INTERVAL.toMillis(), HEALTH_CHECK_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    private void subscribeLoop(byte[][] channels, MessageListener listener) {
        long backoffMillis = MIN_BACKOFF.toMillis();
        while (running) {
            try (Jedis jedis = connector.openDedicatedConnection()) {
                Subscriber current = new Subscriber(listener, channels.length);
                subscriber = current;
                subscriberConnection = jedis;
                jedis.subscribe(current, channels);
                backoffMillis = MIN_BACKOFF.toMillis();
            } catch (RuntimeException exception) {
                if (!running) {
                    return;
                }
                markDisconnected(exception);
                reconnects.increment();
                logger.log(Level.WARNING, "Redis subscription lost, reconnecting in " + backoffMillis + " ms: " + exception.getMessage());
                if (!sleep(backoffMillis)) {
                    return;
                }
                backoffMillis = Math.min(backoffMillis * 2, MAX_BACKOFF.toMillis());
            } finally {
                subscriberConnection = null;
            }
        }
    }

    private void checkHealth() {
        Subscriber current = subscriber;
        if (current == null || !current.isSubscribed()) {
            return;
        }
        try {
            current.ping();
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Redis subscription health check failed: " + exception.getMessage());
            Jedis connection = subscriberConnection;
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void markConnected() {
        connected = true;
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onConnected();
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "Connection listener failed", exception);
            }
        }
    }

    private void markDisconnected(Throwable cause) {
        if (!connected) {
            return;
        }
        connected = false;
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onDisconnected(cause);
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "Connection listener failed", exception);
            }
        }
    }

    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        connected = false;
        if (healthCheck != null) {
            healthCheck.cancel(false);
        }
        Subscriber current = subscriber;
        if (current != null && current.isSubscribed()) {
            try {
                current.unsubscribe();
            } catch (RuntimeException exception) {
                logger.log(Level.FINE, "Unsubscribe failed during close", exception);
            }
        }
        Jedis connection = subscriberConnection;
        if (connection != null) {
            connection.disconnect();
        }
        if (subscriberThread != null) {
            subscriberThread.interrupt();
        }
    }

    private final class Subscriber extends BinaryJedisPubSub {

        private final MessageListener listener;
        private final int expectedChannels;

        private Subscriber(MessageListener listener, int expectedChannels) {
            this.listener = listener;
            this.expectedChannels = expectedChannels;
        }

        @Override
        public void onMessage(byte[] channel, byte[] message) {
            try {
                listener.onMessage(new String(channel, StandardCharsets.UTF_8), message);
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "Message listener failed", exception);
            }
        }

        @Override
        public void onSubscribe(byte[] channel, int subscribedChannels) {
            if (subscribedChannels == expectedChannels) {
                markConnected();
            }
        }
    }
}

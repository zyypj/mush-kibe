package br.com.muxe.core.common.server.heartbeat;

import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.server.ServerShutdownPacket;
import br.com.muxe.core.common.network.server.ServerStatusPacket;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerHeartbeat implements AutoCloseable {

    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(2);

    private final PacketBus bus;
    private final ServerIdentity identity;
    private final ServerLoad load;
    private final ServerDirectory directory;
    private final ScheduledExecutorService scheduler;
    private final Clock clock;
    private final Logger logger;
    private ScheduledFuture<?> task;

    public ServerHeartbeat(PacketBus bus, ServerIdentity identity, ServerLoad load, ServerDirectory directory, ScheduledExecutorService scheduler, Clock clock, Logger logger) {
        this.bus = bus;
        this.identity = identity;
        this.load = load;
        this.directory = directory;
        this.scheduler = scheduler;
        this.clock = clock;
        this.logger = logger;
    }

    public synchronized void start(Duration interval) {
        if (task != null) {
            return;
        }
        task = scheduler.scheduleAtFixedRate(this::beat, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void beat() {
        try {
            ServerStatusPacket packet = new ServerStatusPacket(
                identity.id(),
                identity.group(),
                identity.kind(),
                load.onlinePlayers(),
                load.maxPlayers(),
                load.joinable()
            );
            directory.update(new ServerStatus(identity, packet.onlinePlayers(), packet.maxPlayers(), packet.joinable(), clock.instant(), clock.instant()));
            directory.purgeExpired();
            bus.publish(PacketTarget.broadcast(), packet).exceptionally(error -> {
                logger.log(Level.FINE, "Heartbeat publish failed: " + error.getMessage());
                return null;
            });
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Heartbeat failed", exception);
        }
    }

    @Override
    public synchronized void close() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        try {
            bus.publish(PacketTarget.broadcast(), new ServerShutdownPacket(identity.id())).get(1, TimeUnit.SECONDS);
        } catch (Exception exception) {
            logger.log(Level.FINE, "Shutdown notice not delivered: " + exception.getMessage());
        }
    }
}

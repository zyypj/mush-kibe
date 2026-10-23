package br.com.muxe.core.common.runtime;

import br.com.muxe.core.common.account.AccountModule;
import br.com.muxe.core.common.account.infrastructure.postgres.PostgresAccountRepository;
import br.com.muxe.core.common.concurrent.MuxeExecutors;
import br.com.muxe.core.common.config.settings.MuxeSettings;
import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.middleware.HandlerMiddleware;
import br.com.muxe.core.common.cqrs.middleware.MetricsMiddleware;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.messaging.MessagingConfig;
import br.com.muxe.core.common.messaging.bus.DefaultPacketBus;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.envelope.EnvelopeCodec;
import br.com.muxe.core.common.messaging.idempotency.IdempotencyGuard;
import br.com.muxe.core.common.messaging.idempotency.memory.InMemoryIdempotencyStore;
import br.com.muxe.core.common.messaging.idempotency.redis.RedisIdempotencyStore;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.messaging.security.HmacEnvelopeSigner;
import br.com.muxe.core.common.messaging.transport.ConnectionListener;
import br.com.muxe.core.common.messaging.transport.redis.RedisMessageTransport;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.metrics.export.MetricsConfig;
import br.com.muxe.core.common.metrics.export.MetricsHttpServer;
import br.com.muxe.core.common.metrics.export.PrometheusTextFormat;
import br.com.muxe.core.common.metrics.jvm.JvmMetrics;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.network.NetworkPackets;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.redis.RedisConnector;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerDirectorySync;
import br.com.muxe.core.common.server.heartbeat.ServerHeartbeat;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

public final class CoreRuntime implements AutoCloseable {

    public static final Duration SERVER_TIME_TO_LIVE = Duration.ofSeconds(10);
    public static final String CORE_MODULE = "core";

    private final MuxeSettings settings;
    private final Logger logger;
    private final Clock clock;
    private final Closer closer;
    private final MetricRegistry metrics;
    private final MuxeExecutors executors;
    private final Transactions transactions;
    private final MigrationRunner migrations;
    private final PacketRegistry packetRegistry;
    private final PacketBus packets;
    private final ServerDirectory directory;
    private final CommandBus commands;
    private final QueryBus queries;

    private CoreRuntime(Wiring wiring) {
        this.settings = wiring.settings;
        this.logger = wiring.logger;
        this.clock = wiring.clock;
        this.closer = wiring.closer;
        this.metrics = wiring.metrics;
        this.executors = wiring.executors;
        this.transactions = wiring.transactions;
        this.migrations = wiring.migrations;
        this.packetRegistry = wiring.packetRegistry;
        this.packets = wiring.packets;
        this.directory = wiring.directory;
        this.commands = wiring.commands;
        this.queries = wiring.queries;
    }

    public static CoreRuntime start(MuxeSettings settings, PlatformHooks hooks) {
        Wiring wiring = new Wiring(settings, hooks);
        try {
            wiring.connect();
            return new CoreRuntime(wiring);
        } catch (Exception exception) {
            wiring.closer.close();
            throw new RuntimeStartupException("Muxe runtime failed to start: " + exception.getMessage(), exception);
        }
    }

    public ServerIdentity identity() {
        return settings.server();
    }

    public MuxeSettings settings() {
        return settings;
    }

    public Logger logger() {
        return logger;
    }

    public Clock clock() {
        return clock;
    }

    public MetricRegistry metrics() {
        return metrics;
    }

    public MuxeExecutors executors() {
        return executors;
    }

    public Transactions transactions() {
        return transactions;
    }

    public MigrationRunner migrations() {
        return migrations;
    }

    public PacketRegistry packetRegistry() {
        return packetRegistry;
    }

    public PacketBus packets() {
        return packets;
    }

    public ServerDirectory directory() {
        return directory;
    }

    public CommandBus commands() {
        return commands;
    }

    public QueryBus queries() {
        return queries;
    }

    @Override
    public void close() {
        closer.close();
    }

    private static final class Wiring {

        private final MuxeSettings settings;
        private final PlatformHooks hooks;
        private final Logger logger;
        private final Clock clock = Clock.systemUTC();
        private final Closer closer;
        private final SimpleMetricRegistry metrics = new SimpleMetricRegistry();
        private MuxeExecutors executors;
        private Transactions transactions;
        private MigrationRunner migrations;
        private PacketRegistry packetRegistry;
        private PacketBus packets;
        private ServerDirectory directory;
        private CommandBus commands;
        private QueryBus queries;

        private Wiring(MuxeSettings settings, PlatformHooks hooks) {
            this.settings = settings;
            this.hooks = hooks;
            this.logger = hooks.logger();
            this.closer = new Closer(logger);
        }

        private void connect() throws IOException {
            ServerIdentity identity = settings.server();
            JvmMetrics.register(metrics);
            executors = closer.register(MuxeExecutors.create());

            Database database = closer.register(Database.connect(settings.database(), identity.id(), metrics));
            migrations = new MigrationRunner(database.dataSource());
            if (settings.database().runMigrations()) {
                MigrationReport report = migrations.migrate(CORE_MODULE, CoreRuntime.class.getClassLoader());
                logger.info("Schema '" + report.module() + "' at version " + report.schemaVersion() + " (" + report.migrationsExecuted() + " applied)");
            }
            transactions = new JdbcTransactions(database.dataSource(), hooks.mainThreadGuard(), metrics);

            RedisConnector redis = closer.register(RedisConnector.connect(settings.redis(), "muxe-" + identity.id()));
            RedisMessageTransport transport = closer.register(new RedisMessageTransport(redis, executors.io(), executors.scheduler(), metrics, logger));

            packetRegistry = NetworkPackets.registerAll(new PacketRegistry());
            packets = closer.register(DefaultPacketBus.builder()
                .identity(identity)
                .registry(packetRegistry)
                .transport(transport)
                .codec(envelopeCodec(settings.messaging()))
                .idempotency(new IdempotencyGuard(identity.id(), new InMemoryIdempotencyStore(clock, 100_000), new RedisIdempotencyStore(redis), logger))
                .metrics(metrics)
                .executor(executors.io())
                .scheduler(executors.scheduler())
                .clock(clock)
                .logger(logger)
                .build());

            directory = new ServerDirectory(clock, SERVER_TIME_TO_LIVE);
            ServerDirectorySync.bind(packets, directory);
            metrics.gauge("muxe_network_servers_alive", () -> directory.alive().size());
            metrics.gauge("muxe_network_players_online", directory::onlinePlayers);

            List<HandlerMiddleware> middlewares = List.of(new MetricsMiddleware(metrics));
            commands = new SimpleCommandBus(executors.io(), middlewares);
            queries = new SimpleQueryBus(executors.io(), middlewares);
            AccountModule.register(commands, queries, new PostgresAccountRepository(), transactions, clock);

            ServerHeartbeat heartbeat = closer.register(new ServerHeartbeat(packets, identity, hooks.serverLoad(), directory, executors.scheduler(), clock, logger));
            transport.addConnectionListener(new ConnectionListener() {
                @Override
                public void onConnected() {
                    heartbeat.beat();
                }
            });
            packets.start();
            heartbeat.start(ServerHeartbeat.DEFAULT_INTERVAL);
            startMetricsServer(identity);
        }

        private void startMetricsServer(ServerIdentity identity) throws IOException {
            MetricsConfig config = settings.metrics();
            if (!config.enabled()) {
                return;
            }
            PrometheusTextFormat format = new PrometheusTextFormat(Tags.of("server", identity.id(), "group", identity.group()));
            MetricsHttpServer server = closer.register(MetricsHttpServer.start(
                new InetSocketAddress(config.host(), config.port()),
                () -> format.render(metrics.snapshot()),
                executors.io()
            ));
            logger.info("Metrics available at http://" + config.host() + ":" + server.address().getPort() + MetricsHttpServer.PATH);
        }

        private static EnvelopeCodec envelopeCodec(MessagingConfig config) {
            return new EnvelopeCodec(HmacEnvelopeSigner.of(config.secret(), config.previousSecrets()), config.maxEnvelopeBytes());
        }
    }
}

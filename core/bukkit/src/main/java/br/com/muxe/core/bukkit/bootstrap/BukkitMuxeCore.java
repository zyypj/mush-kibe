package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.runtime.CoreRuntime;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;

final class BukkitMuxeCore implements MuxeCore {

    private final CoreRuntime runtime;
    private final Executor mainThread;
    private final NetworkGateway network;
    private final MessageCatalog messages;

    BukkitMuxeCore(CoreRuntime runtime, Executor mainThread, NetworkGateway network, MessageCatalog messages) {
        this.runtime = runtime;
        this.mainThread = mainThread;
        this.network = network;
        this.messages = messages;
    }

    @Override
    public ServerIdentity identity() {
        return runtime.identity();
    }

    @Override
    public PacketBus packets() {
        return runtime.packets();
    }

    @Override
    public PacketRegistry packetRegistry() {
        return runtime.packetRegistry();
    }

    @Override
    public Duration requestTimeout() {
        return runtime.settings().messaging().requestTimeout();
    }

    @Override
    public CommandBus commands() {
        return runtime.commands();
    }

    @Override
    public QueryBus queries() {
        return runtime.queries();
    }

    @Override
    public Transactions transactions() {
        return runtime.transactions();
    }

    @Override
    public MigrationRunner migrations() {
        return runtime.migrations();
    }

    @Override
    public MetricRegistry metrics() {
        return runtime.metrics();
    }

    @Override
    public ServerDirectory servers() {
        return runtime.directory();
    }

    @Override
    public NetworkGateway network() {
        return network;
    }

    @Override
    public Executor mainThread() {
        return mainThread;
    }

    @Override
    public Executor async() {
        return runtime.executors().io();
    }

    @Override
    public Clock clock() {
        return runtime.clock();
    }

    @Override
    public MessageCatalog messages() {
        return messages;
    }
}

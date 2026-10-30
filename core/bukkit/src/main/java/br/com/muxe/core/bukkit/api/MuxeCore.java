package br.com.muxe.core.bukkit.api;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.permission.PermissionService;
import br.com.muxe.core.bukkit.permission.display.NameTags;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.packet.PacketRegistry;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.store.ExpiringStore;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public interface MuxeCore {

    static MuxeCore get() {
        RegisteredServiceProvider<MuxeCore> registration = Bukkit.getServicesManager().getRegistration(MuxeCore.class);
        if (registration == null) {
            throw new IllegalStateException("Muxe-Core is not enabled on this server");
        }
        return registration.getProvider();
    }

    ServerIdentity identity();

    PacketBus packets();

    PacketRegistry packetRegistry();

    Duration requestTimeout();

    CommandBus commands();

    QueryBus queries();

    Transactions transactions();

    MigrationRunner migrations();

    ExpiringStore store();

    MetricRegistry metrics();

    ServerDirectory servers();

    NetworkGateway network();

    Executor mainThread();

    Executor async();

    Clock clock();

    MessageCatalog messages();

    BuildMode buildMode();

    PermissionService permissions();

    NameTags nameTags();
}

package br.com.muxe.core.proxy.editor;

import br.com.muxe.core.common.account.AccountDetailsModule;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.permission.PermissionModule;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.editor.EditorSessions;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryAccountDetailsRepository;
import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.InMemoryPermissionRepository;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.testing.TestNetwork;
import br.com.muxe.core.proxy.editor.api.ApiRequest;
import br.com.muxe.core.proxy.editor.api.ApiResponse;
import br.com.muxe.core.proxy.editor.api.EditorApi;
import br.com.muxe.core.proxy.editor.bootstrap.PermissionEditor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class EditorFixture implements AutoCloseable {

    public static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");
    public static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    public static final UUID VIP = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    public final TestNetwork network = new TestNetwork();
    public final MutableClock clock = new MutableClock(NOW);
    public final InMemoryPermissionRepository permissions = InMemoryPermissionRepository.seeded();
    public final InMemoryAccountDetailsRepository accounts = new InMemoryAccountDetailsRepository();
    public final InMemoryExpiringStore store = new InMemoryExpiringStore(clock);
    public final SimpleCommandBus commands = new SimpleCommandBus(Runnable::run, List.of());
    public final SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
    public final EditorSessions sessions = new EditorSessions(store, clock, EditorSessions.DEFAULT_DURATION);
    public final PermissionSync sync;
    public final EditorApi api;

    public EditorFixture() {
        PermissionModule.register(commands, queries, permissions, new DirectTransactions(), clock);
        AccountDetailsModule.register(commands, queries, accounts, new DirectTransactions(), clock);
        accounts.add(Account.firstLogin(ADMIN, "tadeu", true, NOW));
        accounts.add(Account.firstLogin(VIP, "joana", false, NOW));
        permissions.saveUser(new PermissionUser(ADMIN, List.of(PermissionNode.group("admin", Optional.empty(), NOW)), Optional.empty()));
        permissions.saveUser(new PermissionUser(VIP, List.of(PermissionNode.group("vip", Optional.empty(), NOW)), Optional.of("vip"), Optional.of("carinha")));
        sync = new PermissionSync(commands, queries, network.node("proxy-1", "proxy", ServerKind.PROXY).start().bus(), Runnable::run, Logger.getLogger("test"));
        sync.start().join();
        api = PermissionEditor.api(sessions, queries, sync, clock, Logger.getLogger("test"));
    }

    public String adminToken() {
        return sessions.open(ADMIN, "tadeu");
    }

    public ApiResponse get(String path, String token) {
        return api.handle(ApiRequest.get(path, Optional.of("Bearer " + token)));
    }

    public ApiResponse post(String path, String token, String body) {
        return api.handle(ApiRequest.post(path, Optional.of("Bearer " + token), body));
    }

    @Override
    public void close() {
        sync.stop();
        network.close();
    }
}

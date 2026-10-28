package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.command.framework.TestCommands;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.permission.PermissionCache;
import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.account.AccountDetailsModule;
import br.com.muxe.core.common.account.AccountModule;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.permission.PermissionModule;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryAccountDetailsRepository;
import br.com.muxe.core.common.testing.InMemoryAccountRepository;
import br.com.muxe.core.common.testing.InMemoryPermissionRepository;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.testing.TestNetwork;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class RankTestKit implements PlayerRanks, AutoCloseable {

    public static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");
    public static final MessageCatalog MESSAGES = MessageCatalog.from(ShopFixtures.CONFIG.getConfigurationSection("messages"));
    public static final String CHAT = ShopFixtures.CONFIG.getString("messages.chat-format");

    public final TestNetwork network = new TestNetwork();
    public final InMemoryPermissionRepository database = InMemoryPermissionRepository.seeded();
    public final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    public final InMemoryAccountDetailsRepository details = new InMemoryAccountDetailsRepository();
    public final MutableClock clock = new MutableClock(NOW);
    public final SimpleCommandBus commands = new SimpleCommandBus(Runnable::run, List.of());
    public final SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
    public final PermissionSync sync;
    public final PermissionCache cache;
    public final Replies replies = new Replies(Runnable::run, MESSAGES, Logger.getLogger("test"));
    public final TargetLookup targets = new TargetLookup(name -> Optional.empty(), queries);
    public final TestCommands framework = new TestCommands(ArgumentTypes.basic(), MESSAGES);

    public RankTestKit() {
        PermissionModule.register(commands, queries, database, new DirectTransactions(), clock);
        AccountModule.register(commands, queries, accounts, new DirectTransactions(), clock);
        AccountDetailsModule.register(commands, queries, details, new DirectTransactions(), clock);
        sync = new PermissionSync(commands, queries, network.node("lobby-1", "lobby", ServerKind.LOBBY).start().bus(), Runnable::run, Logger.getLogger("test"));
        sync.start().join();
        cache = new PermissionCache(sync::data, "lobby", clock);
        sync.onUserChange((id, user) -> cache.put(user, "lobby"));
        sync.onDataChange(data -> cache.refreshAll());
        framework.commands().suggestions(SuggestionRegistry.PLAYERS, sender -> List.of("Ana", "tadeu"));
        new PermissionSuggestions(sync::data, cache::resolved, () -> List.of("muxe.build"), () -> List.of("lobby", "bedwars"), () -> List.of("lobby"))
            .register(framework.commands());
    }

    public static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    public Account account(String name) {
        Account account = Account.firstLogin(uuid(name), name, true, NOW.minusSeconds(86_400));
        accounts.save(account);
        details.add(account);
        return account;
    }

    public void online(String name, PermissionNode... nodes) {
        account(name);
        PermissionUser user = new PermissionUser(uuid(name), List.of(nodes), Optional.empty());
        database.saveUser(user);
        sync.load(uuid(name)).join();
    }

    public PermissionUser stored(String name) {
        return database.user(uuid(name));
    }

    @Override
    public PermissionSync sync() {
        return sync;
    }

    @Override
    public PermissionData data() {
        return sync.data();
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public Optional<ResolvedPermissions> resolved(UUID player) {
        return cache.resolved(player);
    }

    @Override
    public Optional<PermissionUser> user(UUID player) {
        return cache.user(player);
    }

    @Override
    public RankDisplay display(UUID player) {
        return cache.resolved(player)
            .map(resolved -> RankDisplay.of(sync.data(), resolved, cache.user(player).flatMap(PermissionUser::selectedTag)))
            .orElse(RankDisplay.NONE);
    }

    @Override
    public void close() {
        network.close();
    }
}

package br.com.muxe.core.bukkit.credit;

import br.com.muxe.core.common.credit.application.query.FindCreditsQuery;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class CreditBalances {

    private final Function<UUID, CompletableFuture<Long>> loader;
    private final Executor mainThread;
    private final Map<UUID, Long> balances = new ConcurrentHashMap<>();
    private final Set<UUID> tracked = ConcurrentHashMap.newKeySet();

    public CreditBalances(QueryBus queries, Executor mainThread) {
        this(playerId -> queries.ask(new FindCreditsQuery(playerId)), mainThread);
    }

    public CreditBalances(Function<UUID, CompletableFuture<Long>> loader, Executor mainThread) {
        this.loader = loader;
        this.mainThread = mainThread;
    }

    public long of(UUID playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    public void track(UUID playerId) {
        tracked.add(playerId);
        refresh(playerId);
    }

    public void forget(UUID playerId) {
        tracked.remove(playerId);
        balances.remove(playerId);
    }

    public void set(UUID playerId, long balance) {
        if (tracked.contains(playerId)) {
            balances.put(playerId, balance);
        }
    }

    public CompletableFuture<Long> refresh(UUID playerId) {
        return loader.apply(playerId).thenApplyAsync(balance -> {
            set(playerId, balance);
            return balance;
        }, mainThread);
    }

    public void refreshAll() {
        Set.copyOf(tracked).forEach(this::refresh);
    }
}

package br.com.muxe.minigame.bedwars.cosmetic.profile;

import br.com.muxe.core.common.cosmetic.application.query.FindCosmeticsQuery;
import br.com.muxe.core.common.cosmetic.domain.CosmeticProfile;
import br.com.muxe.core.common.credit.application.query.FindCreditsQuery;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public final class CosmeticProfiles {

    public static final String ULTRA_PERMISSION = "muxe.cosmetic.ultra";

    private final QueryBus queries;
    private final Executor mainThread;
    private final String category;
    private final Predicate<UUID> online;
    private final Map<UUID, CosmeticWallet> wallets = new ConcurrentHashMap<>();

    public CosmeticProfiles(QueryBus queries, Executor mainThread, String category, Predicate<UUID> online) {
        this.queries = queries;
        this.mainThread = mainThread;
        this.category = category;
        this.online = online;
    }

    public String category() {
        return category;
    }

    public CompletableFuture<CosmeticWallet> load(UUID playerId, boolean ultra) {
        CompletableFuture<CosmeticProfile> cosmetics = queries.ask(new FindCosmeticsQuery(playerId, category));
        CompletableFuture<Long> credits = queries.ask(new FindCreditsQuery(playerId));
        return cosmetics.thenCombine(credits, (profile, balance) ->
            new CosmeticWallet(profile.owned(), profile.selected().orElse(CosmeticWallet.RANDOM), balance, ultra)
        ).thenApplyAsync(wallet -> {
            if (online.test(playerId)) {
                wallets.put(playerId, wallet);
            }
            return wallet;
        }, mainThread);
    }

    public Optional<CosmeticWallet> of(UUID playerId) {
        return Optional.ofNullable(wallets.get(playerId));
    }

    public void update(UUID playerId, CosmeticWallet wallet) {
        wallets.put(playerId, wallet);
    }

    public void forget(UUID playerId) {
        wallets.remove(playerId);
    }
}

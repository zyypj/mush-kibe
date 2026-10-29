package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsQuery;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class AccountProfiles {

    private final QueryBus queries;
    private final PlayerRanks ranks;

    public AccountProfiles(QueryBus queries, PlayerRanks ranks) {
        this.queries = queries;
        this.ranks = ranks;
    }

    public CompletableFuture<Optional<AccountProfile>> find(FindAccountDetailsQuery query, String fallbackName) {
        return queries.ask(query).thenCompose(details -> {
            if (details.isEmpty()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return ranks.sync().fetch(details.get().account().id()).thenApply(user ->
                Optional.of(AccountProfile.of(details.get(), fallbackName, user, ranks.data(), ranks.clock().instant())));
        });
    }
}

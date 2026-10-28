package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.common.account.application.query.FindAccountByUsernameQuery;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.bukkit.entity.Player;

public final class TargetLookup {

    private final Function<String, Optional<Player>> online;
    private final QueryBus queries;

    public TargetLookup(Function<String, Optional<Player>> online, QueryBus queries) {
        this.online = online;
        this.queries = queries;
    }

    public CompletableFuture<Optional<Target>> find(String name) {
        Optional<Player> player = online.apply(name);
        if (player.isPresent()) {
            return CompletableFuture.completedFuture(Optional.of(new Target(player.get().getUniqueId(), player.get().getName())));
        }
        return queries.ask(new FindAccountByUsernameQuery(name))
            .thenApply(account -> account.map(found -> new Target(found.id(), found.currentUsername().orElse(name))));
    }

    public record Target(UUID id, String name) {

        public Target {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
        }
    }
}

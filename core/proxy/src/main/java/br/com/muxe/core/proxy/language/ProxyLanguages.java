package br.com.muxe.core.proxy.language;

import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.language.application.query.FindLanguageQuery;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.network.language.LanguageChangedPacket;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProxyLanguages implements LanguageLookup {

    private final QueryBus queries;
    private final Logger logger;
    private final Map<UUID, Language> languages = new ConcurrentHashMap<>();
    private final List<Consumer<UUID>> listeners = new CopyOnWriteArrayList<>();

    public ProxyLanguages(QueryBus queries, Logger logger) {
        this.queries = queries;
        this.logger = logger;
    }

    public void register(PacketBus bus) {
        bus.subscribe(LanguageChangedPacket.TYPE, context -> change(context.packet().playerId(), context.packet().language()));
    }

    public CompletableFuture<Language> load(UUID player) {
        return queries.ask(new FindLanguageQuery(player))
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not load the language of " + player + ", using " + Language.DEFAULT.tag(), error);
                return Language.DEFAULT;
            })
            .thenApply(language -> {
                Language found = Objects.requireNonNullElse(language, Language.DEFAULT);
                languages.put(player, found);
                return found;
            });
    }

    public void change(UUID player, Language language) {
        if (languages.computeIfPresent(player, (id, previous) -> language) != null) {
            listeners.forEach(listener -> listener.accept(player));
        }
    }

    public void onChange(Consumer<UUID> listener) {
        listeners.add(listener);
    }

    public void forget(UUID player) {
        languages.remove(player);
    }

    @Override
    public Language of(UUID player) {
        return player == null ? Language.DEFAULT : languages.getOrDefault(player, Language.DEFAULT);
    }
}

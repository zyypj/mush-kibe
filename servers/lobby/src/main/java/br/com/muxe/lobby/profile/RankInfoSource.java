package br.com.muxe.lobby.profile;

import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToLongFunction;

public final class RankInfoSource implements PlayerInfoSource {

    private final Function<UUID, Optional<Tag>> tags;
    private final ToLongFunction<UUID> credits;
    private final Localized<PlayerInfo> fallback;
    private final LanguageLookup languages;

    public RankInfoSource(Function<UUID, Optional<Tag>> tags, ToLongFunction<UUID> credits, Localized<PlayerInfo> fallback, LanguageLookup languages) {
        this.tags = tags;
        this.credits = credits;
        this.fallback = fallback;
        this.languages = languages;
    }

    @Override
    public PlayerInfo of(UUID playerId) {
        return new PlayerInfo(tags.apply(playerId).map(Tag::display).orElseGet(() -> fallback.in(languages.of(playerId)).tag()), credits.applyAsLong(playerId));
    }
}

package br.com.muxe.lobby.profile;

import br.com.muxe.core.common.permission.domain.Tag;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToLongFunction;

public final class RankInfoSource implements PlayerInfoSource {

    private final Function<UUID, Optional<Tag>> tags;
    private final ToLongFunction<UUID> credits;
    private final PlayerInfo fallback;

    public RankInfoSource(Function<UUID, Optional<Tag>> tags, ToLongFunction<UUID> credits, PlayerInfo fallback) {
        this.tags = tags;
        this.credits = credits;
        this.fallback = fallback;
    }

    @Override
    public PlayerInfo of(UUID playerId) {
        return new PlayerInfo(tags.apply(playerId).map(Tag::display).orElse(fallback.tag()), credits.applyAsLong(playerId));
    }
}

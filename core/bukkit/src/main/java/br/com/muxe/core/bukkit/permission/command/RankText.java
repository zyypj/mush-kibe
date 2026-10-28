package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.permission.domain.Lifetime;
import br.com.muxe.core.common.permission.domain.RemainingTime;
import br.com.muxe.core.common.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class RankText {

    public static final String PERMANENT = "rank-permanent";
    public static final String REMAINING = "rank-remaining";

    private RankText() {
    }

    public static String lifetime(MessageCatalog messages, Lifetime lifetime) {
        return lifetime.duration().map(RemainingTime::format).orElseGet(() -> messages.get(PERMANENT));
    }

    public static String remaining(MessageCatalog messages, Optional<Instant> expiresAt, Instant now) {
        return expiresAt
            .map(at -> messages.get(REMAINING, "time", RemainingTime.format(Duration.between(now, at))))
            .orElseGet(() -> messages.get(PERMANENT));
    }

    public static String problem(Result<?> result) {
        return result instanceof Result.Failure<?> failure ? failure.problem().code() : "";
    }
}

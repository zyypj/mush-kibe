package br.com.muxe.core.common.party.domain;

import java.time.Duration;
import java.util.Objects;

public record PartyRules(Duration inviteLifetime, Duration offlineGrace) {

    public static final PartyRules DEFAULT = new PartyRules(Duration.ofSeconds(60), Duration.ofMinutes(3));

    public PartyRules {
        Objects.requireNonNull(inviteLifetime, "inviteLifetime");
        Objects.requireNonNull(offlineGrace, "offlineGrace");
        if (inviteLifetime.isNegative() || inviteLifetime.isZero() || offlineGrace.isNegative() || offlineGrace.isZero()) {
            throw new IllegalArgumentException("Party durations must be positive");
        }
    }
}

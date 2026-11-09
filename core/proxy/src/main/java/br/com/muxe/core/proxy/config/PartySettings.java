package br.com.muxe.core.proxy.config;

import br.com.muxe.core.common.config.source.ConfigSource;
import br.com.muxe.core.common.party.domain.PartyRules;
import java.time.Duration;

public record PartySettings(PartyRules rules, Duration gatherTimeout, Duration playersPeriod) {

    public static PartySettings from(ConfigSource source) {
        return new PartySettings(
            new PartyRules(
                source.duration("party.invite-duration", PartyRules.DEFAULT.inviteLifetime()),
                source.duration("party.offline-grace", PartyRules.DEFAULT.offlineGrace())
            ),
            source.duration("party.gather-timeout", Duration.ofSeconds(3)),
            source.duration("party.players-period", Duration.ofSeconds(3))
        );
    }
}

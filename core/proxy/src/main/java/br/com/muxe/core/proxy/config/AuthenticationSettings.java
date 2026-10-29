package br.com.muxe.core.proxy.config;

import br.com.muxe.core.common.config.source.ConfigSource;
import java.time.Duration;

public record AuthenticationSettings(
    boolean premiumAutoLogin,
    Duration premiumLookupTimeout,
    Duration premiumCacheDuration,
    Duration crackedCacheDuration,
    Duration sessionDuration
) {

    public static AuthenticationSettings from(ConfigSource source) {
        return new AuthenticationSettings(
            source.bool("authentication.premium-auto-login", true),
            source.duration("authentication.premium-lookup-timeout", Duration.ofSeconds(3)),
            source.duration("authentication.premium-cache", Duration.ofHours(6)),
            source.duration("authentication.cracked-cache", Duration.ofMinutes(10)),
            source.duration("authentication.session-duration", Duration.ofHours(24))
        );
    }
}

package br.com.muxe.core.proxy.config;

import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.source.ConfigSource;
import br.com.muxe.core.proxy.geoip.download.MaxMindCredentials;
import java.time.Duration;
import java.util.Optional;

public record GeoIpSettings(boolean enabled, Optional<MaxMindCredentials> credentials, Optional<String> disabledReason, Duration refreshAfter) {

    public static final Duration DEFAULT_REFRESH = Duration.ofDays(7);

    public static GeoIpSettings from(ConfigSource source, SecretResolver secrets) {
        Duration refresh = source.duration("geoip.refresh-after", DEFAULT_REFRESH);
        if (!source.bool("geoip.enabled", true)) {
            return off("geoip.enabled is false", refresh);
        }
        String accountId;
        String licenseKey;
        try {
            accountId = secrets.resolve(source.string("geoip.account-id", ""));
            licenseKey = secrets.resolve(source.string("geoip.license-key", ""));
        } catch (ConfigException exception) {
            return off(exception.getMessage(), refresh);
        }
        if (accountId == null || accountId.isBlank() || licenseKey == null || licenseKey.isBlank()) {
            return off("geoip.account-id and geoip.license-key are empty in the proxy config.yml", refresh);
        }
        return new GeoIpSettings(true, Optional.of(new MaxMindCredentials(accountId.strip(), licenseKey.strip())), Optional.empty(), refresh);
    }

    private static GeoIpSettings off(String reason, Duration refresh) {
        return new GeoIpSettings(false, Optional.empty(), Optional.of(reason), refresh);
    }
}

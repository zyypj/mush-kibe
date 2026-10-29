package br.com.muxe.core.proxy.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.source.MapConfigSource;
import br.com.muxe.core.proxy.geoip.download.MaxMindCredentials;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeoIpSettingsTest {

    @TempDir
    private Path folder;

    @Test
    void theKeysCanBeWrittenInTheConfigFile() {
        MapConfigSource config = new MapConfigSource(Map.of("geoip", Map.of("account-id", 123456, "license-key", " license-value ")));

        GeoIpSettings settings = GeoIpSettings.from(config, new SecretResolver(name -> null));

        assertThat(settings.enabled()).isTrue();
        assertThat(settings.credentials()).contains(new MaxMindCredentials("123456", "license-value"));
        assertThat(settings.refreshAfter()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void theKeysCanComeFromTheEnvironment() {
        SecretResolver secrets = new SecretResolver(Map.of("MUXE_MAXMIND_ACCOUNT_ID", "123456", "MUXE_MAXMIND_LICENSE_KEY", "license-value")::get);
        MapConfigSource config = new MapConfigSource(Map.of("geoip", Map.of("account-id", "env:MUXE_MAXMIND_ACCOUNT_ID", "license-key", "env:MUXE_MAXMIND_LICENSE_KEY")));

        assertThat(GeoIpSettings.from(config, secrets).credentials()).contains(new MaxMindCredentials("123456", "license-value"));
    }

    @Test
    void theShippedConfigLeavesGeoIpOffWithAReason() {
        GeoIpSettings settings = GeoIpSettings.from(ProxyConfigFile.load(folder), new SecretResolver(name -> null));

        assertThat(settings.enabled()).isFalse();
        assertThat(settings.credentials()).isEmpty();
        assertThat(settings.disabledReason()).hasValueSatisfying(reason -> assertThat(reason).contains("geoip.account-id"));
    }

    @Test
    void aMissingEnvironmentVariableTurnsGeoIpOffWithAReason() {
        MapConfigSource config = new MapConfigSource(Map.of("geoip", Map.of("account-id", "env:MUXE_MAXMIND_ACCOUNT_ID", "license-key", "env:MUXE_MAXMIND_LICENSE_KEY")));

        GeoIpSettings settings = GeoIpSettings.from(config, new SecretResolver(name -> null));

        assertThat(settings.enabled()).isFalse();
        assertThat(settings.disabledReason()).hasValueSatisfying(reason -> assertThat(reason).contains("MUXE_MAXMIND_ACCOUNT_ID"));
    }

    @Test
    void geoIpCanBeTurnedOff() {
        SecretResolver secrets = new SecretResolver(name -> "value");
        MapConfigSource config = new MapConfigSource(Map.of("geoip", Map.of("enabled", false, "account-id", "1", "license-key", "2")));

        assertThat(GeoIpSettings.from(config, secrets).enabled()).isFalse();
    }

    @Test
    void theLicenseKeyIsNeverPrinted() {
        MaxMindCredentials credentials = new MaxMindCredentials("123456", "license-value");

        assertThat(credentials.toString()).contains("123456").doesNotContain("license-value");
        assertThat(credentials.basicAuthorization()).isEqualTo("Basic MTIzNDU2OmxpY2Vuc2UtdmFsdWU=");
    }
}

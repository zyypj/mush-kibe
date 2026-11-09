package br.com.muxe.core.proxy.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.source.MapConfigSource;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PartySettingsTest {

    @TempDir
    private Path folder;

    @Test
    void invitesLastSixtySecondsAndOfflineMembersFiveMinutesByDefault() {
        PartySettings settings = PartySettings.from(ProxyConfigFile.load(folder));

        assertThat(settings.rules().inviteLifetime()).isEqualTo(Duration.ofSeconds(60));
        assertThat(settings.rules().offlineGrace()).isEqualTo(Duration.ofMinutes(5));
        assertThat(settings.gatherTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(settings.playersPeriod()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void durationsComeFromTheConfiguration() {
        PartySettings settings = PartySettings.from(new MapConfigSource(Map.of("party", Map.of("invite-duration", "30s", "offline-grace", "2m"))));

        assertThat(settings.rules().inviteLifetime()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.rules().offlineGrace()).isEqualTo(Duration.ofMinutes(2));
    }
}

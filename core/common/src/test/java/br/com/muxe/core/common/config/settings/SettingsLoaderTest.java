package br.com.muxe.core.common.config.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.source.MapConfigSource;
import br.com.muxe.core.common.server.ServerKind;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsLoaderTest {

    private static final String SECRET = "a-messaging-secret-with-32-bytes-or-more";

    private final Map<String, String> environment = new HashMap<>(Map.of(
        "MUXE_DATABASE_PASSWORD", "db-pass",
        "MUXE_MESSAGING_SECRET", SECRET
    ));
    private final SettingsLoader loader = new SettingsLoader(new SecretResolver(environment::get));

    @Test
    void loadsEverySection() {
        MuxeSettings settings = loader.load(new MapConfigSource(config()));

        assertThat(settings.server().id()).isEqualTo("lobby-1");
        assertThat(settings.server().kind()).isEqualTo(ServerKind.LOBBY);
        assertThat(settings.database().password()).isEqualTo("db-pass");
        assertThat(settings.database().connectionTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(settings.redis().port()).isEqualTo(6380);
        assertThat(settings.messaging().secret()).isEqualTo(SECRET);
        assertThat(settings.messaging().requestTimeout()).isEqualTo(Duration.ofMillis(1500));
        assertThat(settings.metrics().enabled()).isFalse();
    }

    @Test
    void failsWhenASecretEnvironmentVariableIsMissing() {
        environment.remove("MUXE_DATABASE_PASSWORD");

        assertThatThrownBy(() -> loader.load(new MapConfigSource(config())))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("MUXE_DATABASE_PASSWORD");
    }

    @Test
    void failsOnAnInvalidServerKind() {
        Map<String, Object> config = config();
        config.put("server", Map.of("id", "lobby-1", "group", "lobby", "kind", "spaceship"));

        assertThatThrownBy(() -> loader.load(new MapConfigSource(config))).isInstanceOf(ConfigException.class);
    }

    private static Map<String, Object> config() {
        Map<String, Object> root = new HashMap<>();
        root.put("server", Map.of("id", "lobby-1", "group", "lobby", "kind", "lobby"));
        root.put("database", Map.of(
            "url", "jdbc:postgresql://localhost:5432/muxe",
            "username", "muxe",
            "password", "env:MUXE_DATABASE_PASSWORD",
            "connection-timeout", "7s"
        ));
        root.put("redis", Map.of("host", "localhost", "port", 6380));
        root.put("messaging", Map.of("secret", "env:MUXE_MESSAGING_SECRET", "request-timeout", "1500ms", "previous-secrets", List.of()));
        root.put("metrics", Map.of("enabled", false));
        return root;
    }
}

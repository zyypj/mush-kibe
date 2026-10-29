package br.com.muxe.core.proxy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.source.MapConfigSource;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EditorSettingsTest {

    @TempDir
    private Path folder;

    @Test
    void theEditorListensOnTheAllocatedPortByDefault() {
        EditorSettings settings = EditorSettings.from(ProxyConfigFile.load(folder));

        assertThat(settings.enabled()).isTrue();
        assertThat(settings.bind()).isEqualTo("0.0.0.0");
        assertThat(settings.port()).isEqualTo(1031);
        assertThat(settings.publicUrl()).isEqualTo("http://151.244.40.191:1031");
        assertThat(settings.maxBodyBytes()).isEqualTo(1024 * 1024);
    }

    @Test
    void aTrailingSlashIsRemovedFromThePublicUrl() {
        MapConfigSource config = new MapConfigSource(Map.of("editor", Map.of("public-url", "https://editor.example.com/")));

        assertThat(EditorSettings.from(config).publicUrl()).isEqualTo("https://editor.example.com");
    }

    @Test
    void invalidPortsAndUrlsAreRejected() {
        assertThatThrownBy(() -> EditorSettings.from(new MapConfigSource(Map.of("editor", Map.of("port", 70000)))))
            .isInstanceOf(ConfigException.class);
        assertThatThrownBy(() -> EditorSettings.from(new MapConfigSource(Map.of("editor", Map.of("public-url", "editor")))))
            .isInstanceOf(ConfigException.class);
    }
}

package br.com.muxe.core.proxy.config;

import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.source.ConfigSource;
import java.net.URI;

public record EditorSettings(
    boolean enabled,
    String bind,
    int port,
    String publicUrl,
    int requestsPerMinute,
    int failedLoginsPerTenMinutes,
    int maxBodyBytes
) {

    public EditorSettings {
        if (port < 1 || port > 65535) {
            throw new ConfigException("editor.port must be between 1 and 65535, got: " + port);
        }
        if (requestsPerMinute < 1 || failedLoginsPerTenMinutes < 1 || maxBodyBytes < 1024) {
            throw new ConfigException("editor limits must be positive and editor.max-body-bytes at least 1024");
        }
        publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        URI uri = URI.create(publicUrl);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new ConfigException("editor.public-url must be a full URL such as http://example.com:1031, got: " + publicUrl);
        }
    }

    public static EditorSettings from(ConfigSource source) {
        return new EditorSettings(
            source.bool("editor.enabled", true),
            source.string("editor.bind", "0.0.0.0"),
            source.integer("editor.port", 1031),
            source.string("editor.public-url", "http://127.0.0.1:1031"),
            source.integer("editor.requests-per-minute", 240),
            source.integer("editor.failed-logins-per-ten-minutes", 20),
            source.integer("editor.max-body-bytes", 1024 * 1024)
        );
    }
}

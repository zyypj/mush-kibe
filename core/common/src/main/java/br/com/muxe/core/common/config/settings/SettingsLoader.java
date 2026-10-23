package br.com.muxe.core.common.config.settings;

import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.secret.SecretResolver;
import br.com.muxe.core.common.config.source.ConfigSource;
import br.com.muxe.core.common.messaging.MessagingConfig;
import br.com.muxe.core.common.messaging.envelope.EnvelopeCodec;
import br.com.muxe.core.common.metrics.export.MetricsConfig;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.redis.RedisConfig;
import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Supplier;

public final class SettingsLoader {

    private final SecretResolver secrets;

    public SettingsLoader(SecretResolver secrets) {
        this.secrets = secrets;
    }

    public MuxeSettings load(ConfigSource source) {
        return new MuxeSettings(
            section("server", () -> server(source)),
            section("database", () -> database(source)),
            section("redis", () -> redis(source)),
            section("messaging", () -> messaging(source)),
            section("metrics", () -> metrics(source))
        );
    }

    private ServerIdentity server(ConfigSource source) {
        String kind = source.string("server.kind").trim().toUpperCase(Locale.ROOT);
        try {
            return new ServerIdentity(source.string("server.id"), source.string("server.group"), ServerKind.valueOf(kind));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException(exception.getMessage(), exception);
        }
    }

    private DatabaseConfig database(ConfigSource source) {
        return new DatabaseConfig(
            source.string("database.url"),
            secrets.resolve(source.string("database.username")),
            secrets.resolve(source.string("database.password")),
            source.integer("database.pool-size", 4),
            source.duration("database.connection-timeout", Duration.ofSeconds(5)),
            source.bool("database.run-migrations", true)
        );
    }

    private RedisConfig redis(ConfigSource source) {
        return new RedisConfig(
            source.string("redis.host", "localhost"),
            source.integer("redis.port", 6379),
            secrets.resolve(source.string("redis.username", "")),
            secrets.resolve(source.string("redis.password", "")),
            source.integer("redis.database", 0),
            source.bool("redis.ssl", false),
            source.integer("redis.pool-size", 8),
            source.duration("redis.timeout", Duration.ofSeconds(2))
        );
    }

    private MessagingConfig messaging(ConfigSource source) {
        return new MessagingConfig(
            secrets.resolve(source.string("messaging.secret")),
            source.stringList("messaging.previous-secrets").stream().map(secrets::resolve).toList(),
            source.duration("messaging.request-timeout", Duration.ofSeconds(3)),
            source.integer("messaging.max-envelope-bytes", EnvelopeCodec.DEFAULT_MAX_BYTES)
        );
    }

    private MetricsConfig metrics(ConfigSource source) {
        return new MetricsConfig(
            source.bool("metrics.enabled", true),
            source.string("metrics.host", "127.0.0.1"),
            source.integer("metrics.port", 9400)
        );
    }

    private static <T> T section(String name, Supplier<T> reader) {
        try {
            return reader.get();
        } catch (ConfigException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ConfigException("Invalid '" + name + "' configuration: " + exception.getMessage(), exception);
        }
    }
}

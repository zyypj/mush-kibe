package br.com.muxe.core.common.config.settings;

import br.com.muxe.core.common.messaging.MessagingConfig;
import br.com.muxe.core.common.metrics.export.MetricsConfig;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.redis.RedisConfig;
import br.com.muxe.core.common.server.ServerIdentity;

public record MuxeSettings(
    ServerIdentity server,
    DatabaseConfig database,
    RedisConfig redis,
    MessagingConfig messaging,
    MetricsConfig metrics
) {
}

package br.com.muxe.core.common.metrics.export;

public record MetricsConfig(boolean enabled, String host, int port) {

    public MetricsConfig {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("Invalid metrics port: " + port);
        }
    }
}

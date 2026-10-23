package br.com.muxe.core.common.metrics;

import java.util.Objects;
import java.util.regex.Pattern;

public record MetricId(String name, Tags tags) {

    private static final Pattern NAME = Pattern.compile("[a-z_][a-z0-9_]*");

    public MetricId {
        Objects.requireNonNull(tags, "tags");
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid metric name: " + name);
        }
    }
}

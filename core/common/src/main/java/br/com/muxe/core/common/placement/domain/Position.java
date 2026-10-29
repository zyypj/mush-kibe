package br.com.muxe.core.common.placement.domain;

import java.util.Locale;
import java.util.Objects;

public record Position(String world, double x, double y, double z, float yaw, float pitch) {

    public Position {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) {
            throw new IllegalArgumentException("A position needs a world");
        }
    }

    public String describe() {
        return String.format(Locale.ROOT, "%s %.1f, %.1f, %.1f", world, x, y, z);
    }
}

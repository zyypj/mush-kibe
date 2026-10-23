package br.com.muxe.lobby.spawn;

import br.com.muxe.core.common.config.source.ConfigSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record SpawnPoint(String world, double x, double y, double z, float yaw, float pitch) {

    public SpawnPoint {
        Objects.requireNonNull(world, "world");
    }

    public static Optional<SpawnPoint> from(ConfigSource source, String path) {
        if (source.find(path + ".world").isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SpawnPoint(
            source.string(path + ".world"),
            number(source, path + ".x"),
            number(source, path + ".y"),
            number(source, path + ".z"),
            (float) number(source, path + ".yaw"),
            (float) number(source, path + ".pitch")
        ));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("world", world);
        values.put("x", x);
        values.put("y", y);
        values.put("z", z);
        values.put("yaw", (double) yaw);
        values.put("pitch", (double) pitch);
        return values;
    }

    private static double number(ConfigSource source, String path) {
        return source.find(path)
            .map(value -> value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value)))
            .orElse(0.0);
    }
}

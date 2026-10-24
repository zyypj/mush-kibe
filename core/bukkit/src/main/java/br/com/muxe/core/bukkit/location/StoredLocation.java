package br.com.muxe.core.bukkit.location;

import br.com.muxe.core.common.config.source.ConfigSource;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.World;

public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public StoredLocation {
        Objects.requireNonNull(world, "world");
    }

    public static StoredLocation of(Location location) {
        Objects.requireNonNull(location.getWorld(), "location world");
        return new StoredLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static Optional<StoredLocation> from(ConfigSource source, String path) {
        if (source.find(path + ".world").isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new StoredLocation(
            source.string(path + ".world"),
            number(source, path + ".x"),
            number(source, path + ".y"),
            number(source, path + ".z"),
            (float) number(source, path + ".yaw"),
            (float) number(source, path + ".pitch")
        ));
    }

    public Optional<Location> resolve(Function<String, World> worlds) {
        return Optional.ofNullable(worlds.apply(world)).map(loaded -> new Location(loaded, x, y, z, yaw, pitch));
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

    public String describe() {
        return String.format(Locale.ROOT, "%s %.1f, %.1f, %.1f (yaw %.0f, pitch %.0f)", world, x, y, z, yaw, pitch);
    }

    private static double number(ConfigSource source, String path) {
        return source.find(path)
            .map(value -> value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value)))
            .orElse(0.0);
    }
}

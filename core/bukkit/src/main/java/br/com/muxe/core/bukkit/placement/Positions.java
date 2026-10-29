package br.com.muxe.core.bukkit.placement;

import br.com.muxe.core.common.placement.domain.Position;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.World;

public final class Positions {

    private Positions() {
    }

    public static Position of(Location location) {
        Objects.requireNonNull(location.getWorld(), "location world");
        return new Position(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static Position standing(Location location) {
        Position position = of(location);
        return new Position(position.world(), position.x(), position.y(), position.z(), position.yaw(), 0f);
    }

    public static Optional<Location> resolve(Position position, Function<String, World> worlds) {
        return Optional.ofNullable(worlds.apply(position.world()))
            .map(world -> new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch()));
    }
}

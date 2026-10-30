package br.com.muxe.minigame.engine.setup.session;

import br.com.muxe.minigame.engine.setup.Point;
import org.bukkit.Location;
import org.bukkit.World;

public final class Points {

    private Points() {
    }

    public static Point of(Location location) {
        return new Point(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static Location toLocation(Point point, World world) {
        return new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
    }
}

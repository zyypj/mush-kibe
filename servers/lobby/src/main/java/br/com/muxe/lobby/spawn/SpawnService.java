package br.com.muxe.lobby.spawn;

import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnService {

    private static final String PATH = "spawn";

    private final JavaPlugin plugin;
    private SpawnPoint spawn;

    public SpawnService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spawn = SpawnPoint.from(new BukkitConfigSource(plugin.getConfig()), PATH).orElse(null);
    }

    public Location location() {
        Optional<Location> configured = Optional.ofNullable(spawn).flatMap(SpawnService::toLocation);
        return configured.orElseGet(() -> Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    public void set(Location location) {
        spawn = new SpawnPoint(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        for (Map.Entry<String, Object> entry : spawn.toMap().entrySet()) {
            plugin.getConfig().set(PATH + "." + entry.getKey(), entry.getValue());
        }
        plugin.saveConfig();
    }

    private static Optional<Location> toLocation(SpawnPoint point) {
        World world = Bukkit.getWorld(point.world());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch()));
    }
}

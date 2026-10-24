package br.com.muxe.lobby.spawn;

import br.com.muxe.core.bukkit.location.ConfigLocation;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnService {

    private static final String PATH = "spawn";

    private final JavaPlugin plugin;
    private final ConfigLocation spawn;

    public SpawnService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spawn = new ConfigLocation(plugin, PATH);
    }

    public Location location() {
        return spawn.location().orElseGet(() -> plugin.getServer().getWorlds().getFirst().getSpawnLocation());
    }

    public void set(Location location) {
        spawn.set(location);
    }
}

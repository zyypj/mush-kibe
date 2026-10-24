package br.com.muxe.core.bukkit.location;

import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigLocation implements LocationSlot {

    private final JavaPlugin plugin;
    private final String path;
    private StoredLocation stored;

    public ConfigLocation(JavaPlugin plugin, String path) {
        this.plugin = plugin;
        this.path = path;
        this.stored = StoredLocation.from(new BukkitConfigSource(plugin.getConfig()), path).orElse(null);
    }

    @Override
    public Optional<StoredLocation> stored() {
        return Optional.ofNullable(stored);
    }

    @Override
    public Optional<Location> location() {
        return stored().flatMap(location -> location.resolve(plugin.getServer()::getWorld));
    }

    @Override
    public void set(Location location) {
        stored = StoredLocation.of(location);
        plugin.getConfig().set(path, null);
        for (Map.Entry<String, Object> entry : stored.toMap().entrySet()) {
            plugin.getConfig().set(path + "." + entry.getKey(), entry.getValue());
        }
        plugin.saveConfig();
    }

    @Override
    public void clear() {
        stored = null;
        plugin.getConfig().set(path, null);
        plugin.saveConfig();
    }
}

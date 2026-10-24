package br.com.muxe.core.bukkit.location;

import java.util.Optional;
import org.bukkit.Location;

public interface LocationSlot {

    Optional<StoredLocation> stored();

    Optional<Location> location();

    void set(Location location);

    void clear();
}

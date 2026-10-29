package br.com.muxe.lobby.spawn;

import org.bukkit.Location;

public record Arrival(Location location, boolean flying) {

    public static Arrival at(Location spawn, boolean canFly, double flyHeight) {
        if (!canFly) {
            return new Arrival(spawn.clone(), false);
        }
        return new Arrival(spawn.clone().add(0, flyHeight, 0), true);
    }
}

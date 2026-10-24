package br.com.muxe.lobby.spawn;

import org.bukkit.entity.Player;

public final class ArrivalService {

    public static final String FLY_PERMISSION = "muxe.lobby.fly";

    private final SpawnService spawn;
    private final double flyHeight;

    public ArrivalService(SpawnService spawn, double flyHeight) {
        this.spawn = spawn;
        this.flyHeight = flyHeight;
    }

    public void send(Player player) {
        boolean canFly = player.hasPermission(FLY_PERMISSION);
        Arrival arrival = Arrival.at(spawn.location(), canFly, flyHeight);
        player.setFallDistance(0);
        player.teleport(arrival.location());
        player.setAllowFlight(canFly);
        player.setFlying(arrival.flying());
    }
}

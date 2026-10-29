package br.com.muxe.lobby.spawn;

import br.com.muxe.core.bukkit.buildmode.BuildModeHook;
import org.bukkit.entity.Player;

public final class FlightBuildHook implements BuildModeHook {

    @Override
    public void left(Player player) {
        player.setAllowFlight(player.hasPermission(ArrivalService.FLY_PERMISSION));
    }
}

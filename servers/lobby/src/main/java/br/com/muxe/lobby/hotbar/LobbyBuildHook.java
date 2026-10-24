package br.com.muxe.lobby.hotbar;

import br.com.muxe.core.bukkit.buildmode.BuildModeHook;
import br.com.muxe.lobby.spawn.ArrivalService;
import br.com.muxe.lobby.visibility.VisibilityService;
import org.bukkit.entity.Player;

public final class LobbyBuildHook implements BuildModeHook {

    private final LobbyHotbar hotbar;
    private final VisibilityService visibility;

    public LobbyBuildHook(LobbyHotbar hotbar, VisibilityService visibility) {
        this.hotbar = hotbar;
        this.visibility = visibility;
    }

    @Override
    public void left(Player player) {
        hotbar.give(player, visibility.of(player.getUniqueId()));
        player.setAllowFlight(player.hasPermission(ArrivalService.FLY_PERMISSION));
    }
}

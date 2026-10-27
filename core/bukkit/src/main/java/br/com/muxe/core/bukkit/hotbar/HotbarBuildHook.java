package br.com.muxe.core.bukkit.hotbar;

import br.com.muxe.core.bukkit.buildmode.BuildModeHook;
import br.com.muxe.core.bukkit.visibility.VisibilityService;
import org.bukkit.entity.Player;

public final class HotbarBuildHook implements BuildModeHook {

    private final Hotbar hotbar;
    private final VisibilityService visibility;

    public HotbarBuildHook(Hotbar hotbar, VisibilityService visibility) {
        this.hotbar = hotbar;
        this.visibility = visibility;
    }

    @Override
    public void left(Player player) {
        hotbar.give(player, visibility.of(player.getUniqueId()));
    }
}

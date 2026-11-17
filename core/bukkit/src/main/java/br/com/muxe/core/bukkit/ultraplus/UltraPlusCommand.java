package br.com.muxe.core.bukkit.ultraplus;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class UltraPlusCommand {

    private final UltraPlusMenus menus;

    public UltraPlusCommand(UltraPlusMenus menus) {
        this.menus = menus;
    }

    @Command(name = "cormais", description = "Opens the colors of the Ultra+ plus.")
    public void plusColor(Player player) {
        menus.openColors(player, Optional.empty());
    }

    @Command(name = "emblemas", aliases = "emblema", description = "Opens the Ultra+ emblems.")
    public void emblems(Player player) {
        menus.openEmblems(player, Optional.empty());
    }
}

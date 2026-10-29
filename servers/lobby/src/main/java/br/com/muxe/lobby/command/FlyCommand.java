package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.spawn.ArrivalService;
import org.bukkit.entity.Player;

@Command(name = "fly", aliases = {"voar"}, description = "Turns lobby flight on or off.")
@Permission(ArrivalService.FLY_PERMISSION)
public final class FlyCommand {

    private final MessageCatalog messages;

    public FlyCommand(MessageCatalog messages) {
        this.messages = messages;
    }

    @Default
    public void toggle(Player player) {
        boolean enabled = !player.getAllowFlight();
        if (!enabled) {
            player.setFlying(false);
        }
        player.setAllowFlight(enabled);
        player.sendMessage(messages.get(enabled ? "fly-enabled" : "fly-disabled"));
    }
}

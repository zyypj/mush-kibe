package br.com.muxe.core.bukkit.customroom.lobby;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import org.bukkit.entity.Player;

@Command(name = "salacustom", aliases = {"salacustomizada", "criarsala"}, description = "Creates a custom room led by you.")
public final class CustomRoomCommand {

    private final CustomRoomLobby lobby;

    public CustomRoomCommand(CustomRoomLobby lobby) {
        this.lobby = lobby;
    }

    @Default
    public void open(Player player) {
        lobby.open(player);
    }
}

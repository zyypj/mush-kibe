package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import org.bukkit.entity.Player;

@Command(name = "lobby", aliases = {"hub", "l"}, description = "Sends you back to a lobby.")
public final class LobbyCommand {

    private final MuxeCore core;
    private final String lobbyGroup;

    public LobbyCommand(MuxeCore core, String lobbyGroup) {
        this.core = core;
        this.lobbyGroup = lobbyGroup;
    }

    @Default
    public void send(Player player) {
        core.network().connectToGroup(player.getUniqueId(), lobbyGroup).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(core.messages().get("connect-failed")));
            return null;
        });
    }
}

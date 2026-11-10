package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import java.util.Optional;
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
        Optional<String> lobby = core.servers().lobbyFor(core.identity(), lobbyGroup);
        if (lobby.isEmpty()) {
            player.sendMessage(core.messages().get(player, lobbyGroup.equals(core.identity().group()) ? "no-other-lobby" : "connect-failed"));
            return;
        }
        core.network().connectToServer(player.getUniqueId(), lobby.get()).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(core.messages().get(player, "connect-failed")));
            return null;
        });
    }
}

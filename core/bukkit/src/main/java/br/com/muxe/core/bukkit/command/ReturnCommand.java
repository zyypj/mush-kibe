package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.common.match.MatchReturns;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

@Command(name = "retornar", aliases = "reconectar", description = "Sends you back to the match you left.")
public final class ReturnCommand {

    private final MuxeCore core;
    private final MatchReturns returns;

    public ReturnCommand(MuxeCore core, MatchReturns returns) {
        this.core = core;
        this.returns = returns;
    }

    @Default
    public void back(Player player) {
        UUID id = player.getUniqueId();
        CompletableFuture.supplyAsync(() -> returns.serverOf(id), core.async())
            .whenCompleteAsync((server, error) -> send(player, error == null ? server : Optional.empty()), core.mainThread());
    }

    private void send(Player player, Optional<String> server) {
        if (!player.isOnline()) {
            return;
        }
        if (server.isEmpty() || server.get().equals(core.identity().id())) {
            player.sendMessage(core.messages().get("nothing-to-return"));
            return;
        }
        core.network().connectToServer(player.getUniqueId(), server.get()).exceptionally(failure -> {
            core.mainThread().execute(() -> player.sendMessage(core.messages().get("connect-failed")));
            return null;
        });
    }
}

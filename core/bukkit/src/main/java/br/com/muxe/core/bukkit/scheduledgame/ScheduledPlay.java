package br.com.muxe.core.bukkit.scheduledgame;

import br.com.muxe.core.bukkit.network.NetworkGateway;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class ScheduledPlay {

    public static final String NONE = "scheduled-game-none";

    private final ScheduledGames games;
    private final NetworkGateway network;
    private final Executor async;
    private final Executor mainThread;
    private final MessageCatalog messages;

    public ScheduledPlay(ScheduledGames games, NetworkGateway network, Executor async, Executor mainThread, MessageCatalog messages) {
        this.games = games;
        this.network = network;
        this.async = async;
        this.mainThread = mainThread;
        this.messages = messages;
    }

    public void play(Player player, String id) {
        Optional<ScheduledGame> game = games.game(id);
        if (game.isEmpty()) {
            player.sendMessage(messages.get(player, NONE));
            return;
        }
        Optional<Consumer<Player>> here = games.here(id);
        if (here.isPresent()) {
            here.get().accept(player);
            return;
        }
        UUID playerId = player.getUniqueId();
        CompletableFuture.supplyAsync(() -> games.isOpen(id) || games.hasRooms(id), async).thenAcceptAsync(open -> {
            if (!player.isOnline()) {
                return;
            }
            if (!open) {
                player.sendMessage(messages.get(player, NONE));
                return;
            }
            CompletableFuture.runAsync(() -> games.rememberJoin(playerId, id), async)
                .thenCompose(ignored -> network.connectToGroup(playerId, game.get().lobbyGroup()))
                .exceptionally(error -> {
                    mainThread.execute(() -> player.sendMessage(messages.get(player, "connect-failed")));
                    return null;
                });
        }, mainThread);
    }
}

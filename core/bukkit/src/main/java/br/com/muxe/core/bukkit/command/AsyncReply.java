package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.result.Result;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class AsyncReply {

    public static final String FAILED = "placement-failed";

    private final Executor mainThread;
    private final MessageCatalog messages;
    private final Logger logger;

    public AsyncReply(Executor mainThread, MessageCatalog messages, Logger logger) {
        this.mainThread = mainThread;
        this.messages = messages;
        this.logger = logger;
    }

    public <T> void then(Player player, CompletableFuture<Result<T>> change, Consumer<T> onSuccess) {
        change.whenCompleteAsync((result, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "A change asked by " + player.getName() + " failed", error);
            }
            if (error != null || result.isFailure()) {
                if (player.isOnline()) {
                    player.sendMessage(messages.get(FAILED));
                }
                return;
            }
            if (player.isOnline()) {
                onSuccess.accept(result.orElseThrow());
            }
        }, mainThread);
    }
}

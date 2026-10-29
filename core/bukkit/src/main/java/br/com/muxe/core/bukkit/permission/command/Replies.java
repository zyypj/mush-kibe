package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Replies {

    public static final String FAILED = "rank-failed";

    private final Executor mainThread;
    private final MessageCatalog messages;
    private final Logger logger;

    public Replies(Executor mainThread, MessageCatalog messages, Logger logger) {
        this.mainThread = mainThread;
        this.messages = messages;
        this.logger = logger;
    }

    public MessageCatalog messages() {
        return messages;
    }

    public <T> void then(CommandSender sender, CompletableFuture<T> work, Consumer<T> onDone) {
        work.whenCompleteAsync((value, error) -> {
            if (sender instanceof Player player && !player.isOnline()) {
                return;
            }
            if (error != null) {
                logger.log(Level.WARNING, "A rank command of " + sender.getName() + " failed", error);
                sender.sendMessage(messages.get(FAILED));
                return;
            }
            try {
                onDone.accept(value);
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "A rank command of " + sender.getName() + " failed", exception);
                sender.sendMessage(messages.get(FAILED));
            }
        }, mainThread);
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        messages.lines(key, placeholders).forEach(sender::sendMessage);
    }
}

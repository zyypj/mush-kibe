package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlayCommand implements CommandExecutor {

    private final MuxeCore core;
    private final MessageCatalog messages;
    private final List<String> games;

    public PlayCommand(MuxeCore core, MessageCatalog messages, List<String> games) {
        this.core = core;
        this.messages = messages;
        this.games = List.copyOf(games);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(messages.get("play-usage", "games", String.join(", ", games)));
            return true;
        }
        String game = args[0].toLowerCase(Locale.ROOT);
        if (!games.contains(game)) {
            player.sendMessage(messages.get("play-unknown", "games", String.join(", ", games)));
            return true;
        }
        player.sendMessage(messages.get("play-connecting", "game", game));
        core.network().connectToGroup(player.getUniqueId(), game).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(messages.get("play-failed")));
            return null;
        });
        return true;
    }
}

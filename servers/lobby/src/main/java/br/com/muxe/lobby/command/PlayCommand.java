package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggestions;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;

@Command(name = "jogar", aliases = "play", description = "Sends you to a game.")
public final class PlayCommand {

    private static final String GAMES = "games";

    private final MuxeCore core;
    private final MessageCatalog messages;
    private final List<String> games;

    public PlayCommand(MuxeCore core, MessageCatalog messages, List<String> games) {
        this.core = core;
        this.messages = messages;
        this.games = List.copyOf(games);
    }

    @Default
    public void play(Player player, @Arg("modo") @Suggest(provider = GAMES) String typedGame) {
        String game = typedGame.toLowerCase(Locale.ROOT);
        if (!games.contains(game)) {
            player.sendMessage(messages.get("play-unknown", "games", String.join(", ", games)));
            return;
        }
        core.network().connectToGroup(player.getUniqueId(), game).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(messages.get("play-failed")));
            return null;
        });
    }

    @Usage
    public void usage(Player player) {
        player.sendMessage(messages.get("play-usage", "games", String.join(", ", games)));
    }

    @Suggestions(GAMES)
    public List<String> games() {
        return games;
    }
}

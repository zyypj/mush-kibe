package br.com.muxe.core.bukkit.command.framework.argument;

import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import java.util.Collection;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlayerArgument implements ArgumentType<Player> {

    private final Server server;

    public PlayerArgument(Server server) {
        this.server = server;
    }

    @Override
    public ArgumentResult<Player> parse(CommandSender sender, String input) {
        Player player = server.getPlayerExact(input);
        if (player == null || !visible(sender, player)) {
            return ArgumentResult.rejected(CommandMessages.PLAYER_OFFLINE, "input", input);
        }
        return ArgumentResult.of(player);
    }

    @Override
    public Collection<String> suggest(CommandSender sender) {
        return server.getOnlinePlayers().stream()
            .filter(player -> visible(sender, player))
            .map(Player::getName)
            .toList();
    }

    private static boolean visible(CommandSender sender, Player player) {
        return !(sender instanceof Player viewer) || viewer.canSee(player);
    }
}

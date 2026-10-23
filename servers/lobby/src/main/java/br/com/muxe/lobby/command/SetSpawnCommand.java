package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.spawn.SpawnService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetSpawnCommand implements CommandExecutor {

    public static final String PERMISSION = "muxe.lobby.setspawn";

    private final SpawnService spawn;
    private final MessageCatalog messages;

    public SetSpawnCommand(SpawnService spawn, MessageCatalog messages) {
        this.spawn = spawn;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(messages.get("no-permission"));
            return true;
        }
        spawn.set(player.getLocation());
        player.sendMessage(messages.get("spawn-set"));
        return true;
    }
}

package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LobbyCommand implements CommandExecutor {

    private final MuxeCore core;
    private final String lobbyGroup;

    public LobbyCommand(MuxeCore core, String lobbyGroup) {
        this.core = core;
        this.lobbyGroup = lobbyGroup;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageCatalog messages = core.messages();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }
        core.network().connectToGroup(player.getUniqueId(), lobbyGroup).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(messages.get("connect-failed")));
            return null;
        });
        return true;
    }
}

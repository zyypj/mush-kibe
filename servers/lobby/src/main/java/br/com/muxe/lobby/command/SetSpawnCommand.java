package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.spawn.SpawnService;
import org.bukkit.entity.Player;

@Command(name = "setspawn", description = "Sets the lobby spawn at your position.")
@Permission(SetSpawnCommand.PERMISSION)
public final class SetSpawnCommand {

    public static final String PERMISSION = "muxe.lobby.setspawn";

    private final SpawnService spawn;
    private final MessageCatalog messages;

    public SetSpawnCommand(SpawnService spawn, MessageCatalog messages) {
        this.spawn = spawn;
        this.messages = messages;
    }

    @Default
    public void set(Player player) {
        spawn.set(player.getLocation());
        player.sendMessage(messages.get("spawn-set"));
    }
}

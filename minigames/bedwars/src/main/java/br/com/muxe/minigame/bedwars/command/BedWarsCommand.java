package br.com.muxe.minigame.bedwars.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggestions;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.room.RoomService;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;

@Command(name = "bedwars", aliases = "bw", description = "Joins a BedWars mode.", usage = "bedwars-usage")
public final class BedWarsCommand {

    public static final String ADMIN = "muxe.bedwars.admin";

    private static final String MODES = "modes";

    private final RoomService rooms;
    private final MessageCatalog messages;

    public BedWarsCommand(RoomService rooms, MessageCatalog messages) {
        this.rooms = rooms;
        this.messages = messages;
    }

    @Subcommand("entrar")
    public void join(Player player, @Arg("modo") @Suggest(provider = MODES) String mode) {
        String id = mode.toLowerCase(Locale.ROOT);
        if (!rooms.modes().contains(id)) {
            player.sendMessage(messages.get("unknown-mode", "modes", String.join(", ", rooms.modes())));
            return;
        }
        rooms.switchTo(player, id);
    }

    @Subcommand("iniciar")
    @Permission(ADMIN)
    public void start(Player player) {
        player.sendMessage(messages.get(rooms.forceStart(player) ? "forced-start" : "cannot-force-start"));
    }

    @Suggestions(MODES)
    public List<String> modes() {
        return rooms.modes();
    }
}

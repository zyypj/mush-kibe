package br.com.muxe.minigame.api.customroom.bukkit;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.api.customroom.CustomRooms;
import org.bukkit.entity.Player;

@Command(name = "menu", aliases = "sala", description = "Opens the menu of your custom room.")
public final class RoomMenuCommand {

    private final CustomRooms<?> rooms;
    private final RoomMenu<?> menu;
    private final MessageCatalog messages;

    RoomMenuCommand(CustomRooms<?> rooms, RoomMenu<?> menu, MessageCatalog messages) {
        this.rooms = rooms;
        this.menu = menu;
        this.messages = messages;
    }

    @Default
    public void open(Player player) {
        if (rooms.of(player.getUniqueId()).isEmpty()) {
            player.sendMessage(messages.get("custom-room-not-in-room"));
            return;
        }
        if (rooms.leading(player.getUniqueId()).isEmpty()) {
            player.sendMessage(messages.get("custom-room-only-leader"));
            return;
        }
        menu.open(player);
    }
}

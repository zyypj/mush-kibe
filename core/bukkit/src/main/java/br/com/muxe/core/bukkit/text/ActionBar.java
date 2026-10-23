package br.com.muxe.core.bukkit.text;

import br.com.muxe.core.bukkit.nms.LegacyPackets;
import org.bukkit.entity.Player;

public final class ActionBar {

    private ActionBar() {
    }

    public static boolean send(Player player, String message) {
        return LegacyPackets.sendActionBar(player, Colors.translate(message));
    }

    public static boolean isSupported() {
        return LegacyPackets.isAvailable();
    }
}

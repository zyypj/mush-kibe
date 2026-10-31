package br.com.muxe.core.bukkit.greeting;

import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class LeftoverTitleCleaner implements Listener {

    private final Consumer<Player> clearTitle;

    public LeftoverTitleCleaner(Consumer<Player> clearTitle) {
        this.clearTitle = clearTitle;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        clearTitle.accept(event.getPlayer());
    }
}

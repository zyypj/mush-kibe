package br.com.muxe.core.bukkit.party;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PartyPresenceListener implements Listener {

    private final PartyClient parties;
    private final Function<UUID, RankDisplay> displays;

    public PartyPresenceListener(PartyClient parties, Function<UUID, RankDisplay> displays) {
        this.parties = parties;
        this.displays = displays;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        announce(event.getPlayer());
    }

    public void announce(Player player) {
        parties.announce(player.getUniqueId(), displays.apply(player.getUniqueId()).coloredName(player.getName()));
    }
}

package br.com.muxe.minigame.api.matchmaking;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.scheduledgame.ScheduledGame;
import br.com.muxe.core.bukkit.scheduledgame.ScheduledGames;
import br.com.muxe.core.bukkit.scheduledgame.ScheduledPlay;
import br.com.muxe.minigame.api.network.RoomInfo;
import br.com.muxe.minigame.api.network.RoomsPacket;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class ScheduledJoins implements Listener {

    public static final long ARRIVAL_DELAY_TICKS = 10L;

    private final Plugin plugin;
    private final MuxeCore core;
    private final String game;
    private final MatchConnector connector;

    private ScheduledJoins(Plugin plugin, MuxeCore core, String game, MatchConnector connector) {
        this.plugin = plugin;
        this.core = core;
        this.game = game;
        this.connector = connector;
    }

    public static ScheduledJoins register(Plugin plugin, MuxeCore core, String game, MatchConnector connector, boolean joinOnArrival) {
        ScheduledJoins joins = new ScheduledJoins(plugin, core, game, connector);
        core.scheduledGames().all().stream()
            .filter(scheduled -> scheduled.game().equals(game))
            .forEach(scheduled -> core.scheduledGames().playHere(scheduled.id(), player -> joins.join(player, scheduled)));
        if (joinOnArrival) {
            plugin.getServer().getPluginManager().registerEvents(joins, plugin);
        }
        return joins;
    }

    public static List<String> waiting(ScheduledGames games, String game, Collection<RoomInfo> rooms) {
        return games.all().stream()
            .filter(scheduled -> scheduled.game().equals(game))
            .filter(scheduled -> rooms.stream().anyMatch(room -> room.mode().equals(scheduled.mode()) && room.hasRoom()))
            .map(ScheduledGame::id)
            .toList();
    }

    public static boolean hasRoom(Collection<RoomsPacket> servers, String mode) {
        return servers.stream().flatMap(server -> server.rooms().stream()).anyMatch(room -> room.mode().equals(mode) && room.hasRoom());
    }

    public void join(Player player, ScheduledGame scheduled) {
        if (!core.scheduledGames().isOpen(scheduled.id()) && !hasRoom(connector.directory().alive(), scheduled.mode())) {
            player.sendMessage(core.messages().get(player, ScheduledPlay.NONE));
            return;
        }
        connector.join(player, scheduled.mode(), Optional.empty());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        CompletableFuture.supplyAsync(() -> core.scheduledGames().takeJoin(id), core.async()).thenAcceptAsync(wanted -> wanted
            .flatMap(core.scheduledGames()::game)
            .filter(scheduled -> scheduled.game().equals(game))
            .ifPresent(scheduled -> plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    join(player, scheduled);
                }
            }, ARRIVAL_DELAY_TICKS)), core.mainThread());
    }
}

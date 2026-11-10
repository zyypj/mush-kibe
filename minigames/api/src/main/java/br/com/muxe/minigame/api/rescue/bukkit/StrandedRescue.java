package br.com.muxe.minigame.api.rescue.bukkit;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.minigame.api.rescue.StrandedPlayers;
import br.com.muxe.minigame.api.runtime.MatchService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class StrandedRescue {

    public static final long CHECK_TICKS = 20L;
    public static final String MESSAGE = "rescued-to-lobby";

    private final Plugin plugin;
    private final MuxeCore core;
    private final MatchService<?, ?> matches;
    private final String lobbyGroup;
    private final StrandedPlayers stranded = new StrandedPlayers();
    private BukkitTask task;

    private StrandedRescue(Plugin plugin, MuxeCore core, MatchService<?, ?> matches, String lobbyGroup) {
        this.plugin = plugin;
        this.core = core;
        this.matches = matches;
        this.lobbyGroup = lobbyGroup;
    }

    public static StrandedRescue start(Plugin plugin, MuxeCore core, MatchService<?, ?> matches, String lobbyGroup) {
        StrandedRescue rescue = new StrandedRescue(plugin, core, matches, lobbyGroup);
        rescue.task = plugin.getServer().getScheduler().runTaskTimer(plugin, rescue::check, CHECK_TICKS, CHECK_TICKS);
        return rescue;
    }

    public void rescue(Player player) {
        stranded.rescued(player.getUniqueId(), core.clock().instant());
        sendBack(player);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void check() {
        List<UUID> outside = plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> !matches.isMatchWorld(player.getLocation()))
            .map(Player::getUniqueId)
            .toList();
        for (UUID id : stranded.toRescue(outside, core.clock().instant())) {
            Optional.ofNullable(plugin.getServer().getPlayer(id)).ifPresent(this::sendBack);
        }
    }

    private void sendBack(Player player) {
        matches.rescue(player.getUniqueId());
        player.sendMessage(core.messages().get(player, MESSAGE));
        core.network().connectToGroup(player.getUniqueId(), lobbyGroup);
    }
}

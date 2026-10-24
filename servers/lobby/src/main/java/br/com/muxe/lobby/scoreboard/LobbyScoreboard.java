package br.com.muxe.lobby.scoreboard;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.scoreboard.Sidebar;
import br.com.muxe.lobby.profile.PlayerInfoSource;
import br.com.muxe.lobby.selector.LobbyNumber;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LobbyScoreboard implements Runnable {

    private final MuxeCore core;
    private final ScoreboardTemplate template;
    private final PlayerInfoSource info;
    private final Map<UUID, Sidebar> sidebars = new ConcurrentHashMap<>();

    public LobbyScoreboard(MuxeCore core, ScoreboardTemplate template, PlayerInfoSource info) {
        this.core = core;
        this.template = template;
        this.info = info;
    }

    public void show(Player player) {
        Sidebar sidebar = Sidebar.create(player);
        sidebars.put(player.getUniqueId(), sidebar);
        render(player, sidebar);
    }

    public void forget(Player player) {
        sidebars.remove(player.getUniqueId());
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Sidebar sidebar = sidebars.get(player.getUniqueId());
            if (sidebar != null) {
                render(player, sidebar);
            }
        }
    }

    private void render(Player player, Sidebar sidebar) {
        sidebar.title(template.title());
        sidebar.lines(template.render(info.of(player.getUniqueId()), LobbyNumber.label(core.identity().id()), core.servers().onlinePlayers()));
    }
}

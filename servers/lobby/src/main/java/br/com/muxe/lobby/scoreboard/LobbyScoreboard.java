package br.com.muxe.lobby.scoreboard;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.scoreboard.Sidebar;
import br.com.muxe.core.bukkit.scoreboard.SidebarContent;
import br.com.muxe.core.bukkit.selector.lobby.LobbyNumber;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LobbyScoreboard implements Runnable {

    private final MuxeCore core;
    private final LobbyBoard board;
    private final Map<UUID, Sidebar> sidebars = new ConcurrentHashMap<>();

    public LobbyScoreboard(MuxeCore core, LobbyBoard board) {
        this.core = core;
        this.board = board;
    }

    public void show(Player player) {
        Sidebar sidebar = Sidebar.create(player);
        sidebars.put(player.getUniqueId(), sidebar);
        render(player, sidebar);
    }

    public void refresh(Player player) {
        Sidebar sidebar = sidebars.get(player.getUniqueId());
        if (sidebar != null) {
            render(player, sidebar);
        }
    }

    public void forget(Player player) {
        sidebars.remove(player.getUniqueId());
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private void render(Player player, Sidebar sidebar) {
        SidebarContent content = core.sidebars().contentFor(player)
            .orElseGet(() -> board.content(player.getUniqueId(), LobbyNumber.label(core.identity().id()), core.servers().onlinePlayers()));
        sidebar.title(content.title());
        sidebar.lines(content.lines());
    }
}

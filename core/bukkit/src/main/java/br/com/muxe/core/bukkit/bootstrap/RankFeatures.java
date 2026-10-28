package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.permission.PermissionService;
import br.com.muxe.core.bukkit.permission.display.ChatFormatListener;
import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.core.bukkit.permission.display.NameTagTeams;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class RankFeatures {

    private static final String CHAT_FORMAT = "chat-format";

    private final NameTagTeams teams;

    private RankFeatures(NameTagTeams teams) {
        this.teams = teams;
    }

    static RankFeatures start(JavaPlugin plugin, MuxeCore core, FileConfiguration config, Commands commands) {
        PermissionService permissions = core.permissions();
        NameTagTeams teams = new NameTagTeams(plugin.getServer(), player -> NameTag.of(
            permissions.resolved(player).flatMap(resolved -> resolved.primaryGroup()),
            permissions.display(player)
        ));
        permissions.onChange(teams::update);
        plugin.getServer().getPluginManager().registerEvents(teams, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, teams, NameTagTeams.PERIOD_TICKS, NameTagTeams.PERIOD_TICKS);
        String chatFormat = config.getString("messages." + CHAT_FORMAT, "{prefix}{color}{player}&f: {message}");
        plugin.getServer().getPluginManager().registerEvents(new ChatFormatListener(permissions::display, chatFormat), plugin);
        return new RankFeatures(teams);
    }

    void stop() {
        teams.clear();
    }
}

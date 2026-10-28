package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.permission.PermissionService;
import br.com.muxe.core.bukkit.permission.command.PermissionSuggestions;
import br.com.muxe.core.bukkit.permission.command.Replies;
import br.com.muxe.core.bukkit.permission.command.TagCommand;
import br.com.muxe.core.bukkit.permission.command.TagList;
import br.com.muxe.core.bukkit.permission.command.TargetLookup;
import br.com.muxe.core.bukkit.permission.display.ChatFormatListener;
import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.core.bukkit.permission.display.NameTagTeams;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import java.util.Optional;
import java.util.TreeSet;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

final class RankFeatures {

    private static final String CHAT_FORMAT = "messages.chat-format";
    private static final String DEFAULT_CHAT_FORMAT = "{prefix}{color}{player}&f: {message}";

    private final NameTagTeams teams;

    private RankFeatures(NameTagTeams teams) {
        this.teams = teams;
    }

    static RankFeatures start(JavaPlugin plugin, MuxeCore core, FileConfiguration config, Commands commands) {
        Server server = plugin.getServer();
        PermissionService permissions = core.permissions();
        NameTagTeams teams = new NameTagTeams(server, player -> NameTag.of(
            permissions.resolved(player).flatMap(ResolvedPermissions::primaryGroup),
            permissions.display(player)
        ));
        permissions.onChange(teams::update);
        server.getPluginManager().registerEvents(teams, plugin);
        server.getScheduler().runTaskTimer(plugin, teams, NameTagTeams.PERIOD_TICKS, NameTagTeams.PERIOD_TICKS);
        String chatFormat = config.getString(CHAT_FORMAT, DEFAULT_CHAT_FORMAT);
        server.getPluginManager().registerEvents(new ChatFormatListener(permissions::display, chatFormat), plugin);

        new PermissionSuggestions(
            permissions::data,
            permissions::resolved,
            () -> server.getPluginManager().getPermissions().stream().map(Permission::getName).toList(),
            () -> serverGroups(core),
            () -> server.getWorlds().stream().map(World::getName).toList()
        ).register(commands);
        Replies replies = new Replies(core.mainThread(), core.messages(), plugin.getLogger());
        TargetLookup targets = new TargetLookup(name -> Optional.ofNullable(server.getPlayerExact(name)), core.queries());
        commands.register(new TagCommand(permissions, targets, replies, new TagList(core.messages(), chatFormat)));
        return new RankFeatures(teams);
    }

    void stop() {
        teams.clear();
    }

    private static TreeSet<String> serverGroups(MuxeCore core) {
        TreeSet<String> groups = new TreeSet<>();
        groups.add(core.identity().group());
        core.servers().alive().forEach(status -> groups.add(status.identity().group()));
        return groups;
    }
}

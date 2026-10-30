package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.account.AccountCommands;
import br.com.muxe.core.bukkit.account.AccountLines;
import br.com.muxe.core.bukkit.account.AccountMenu;
import br.com.muxe.core.bukkit.account.AccountProfiles;
import br.com.muxe.core.bukkit.account.AccountSettings;
import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.permission.PermissionService;
import br.com.muxe.core.bukkit.permission.command.EditorCommand;
import br.com.muxe.core.bukkit.permission.command.MedalCommand;
import br.com.muxe.core.bukkit.permission.command.MedalList;
import br.com.muxe.core.bukkit.permission.command.PermissionCommand;
import br.com.muxe.core.bukkit.permission.command.PermissionSuggestions;
import br.com.muxe.core.bukkit.permission.command.RankCommand;
import br.com.muxe.core.bukkit.permission.command.RankReports;
import br.com.muxe.core.bukkit.permission.command.Replies;
import br.com.muxe.core.bukkit.permission.command.TagCommand;
import br.com.muxe.core.bukkit.permission.command.TagList;
import br.com.muxe.core.bukkit.permission.command.TargetLookup;
import br.com.muxe.core.bukkit.permission.display.ChatFormatListener;
import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.core.bukkit.permission.display.NameTagTeams;
import br.com.muxe.core.bukkit.permission.display.NameTags;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.editor.EditorSessions;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

final class RankFeatures {

    private static final String CHAT_FORMAT = "messages.chat-format";
    private static final String ACCOUNT_MENU = "menus.account";
    private static final String EDITOR_URL = "permissions.editor-url";
    private static final String DEFAULT_EDITOR_URL = "http://151.244.40.191:1031";
    private static final String DEFAULT_CHAT_FORMAT = "{medal}{prefix}{color}{player}: &f{message}";

    private final NameTagTeams teams;

    private RankFeatures(NameTagTeams teams) {
        this.teams = teams;
    }

    static RankFeatures start(JavaPlugin plugin, MuxeCore core, FileConfiguration config, Commands commands) {
        Server server = plugin.getServer();
        PermissionService permissions = core.permissions();
        NameTags nameTags = core.nameTags();
        NameTagTeams teams = new NameTagTeams(server, player -> nameTags.resolve(player, rank -> NameTag.of(
            permissions.resolved(rank).flatMap(ResolvedPermissions::primaryGroup),
            permissions.display(rank)
        )));
        nameTags.attach(teams::update);
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
        AccountSettings accountSettings = AccountSettings.from(Objects.requireNonNull(config.getConfigurationSection(ACCOUNT_MENU), "Missing configuration section " + ACCOUNT_MENU));
        AccountLines accountLines = new AccountLines(core.messages(), accountSettings.chatDates());
        RankReports reports = new RankReports(core.messages(), permissions::data, core.clock(), accountSettings.chatDates());
        EditorSessions editor = new EditorSessions(core.store(), core.clock(), EditorSessions.DEFAULT_DURATION);
        commands.register(
            new TagCommand(permissions, targets, replies, new TagList(core.messages(), chatFormat)),
            new MedalCommand(permissions, targets, replies, new MedalList(core.messages(), chatFormat)),
            new AccountCommands(new AccountProfiles(core.queries(), permissions), accountLines, new AccountMenu(accountSettings, accountLines, core.messages()), replies),
            new RankCommand(permissions, targets, replies, reports),
            new PermissionCommand(permissions, targets, replies, reports),
            new EditorCommand(editor, core.async(), config.getString(EDITOR_URL, DEFAULT_EDITOR_URL), replies)
        );
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

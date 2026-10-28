package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.Lifetime;
import br.com.muxe.core.common.permission.domain.Medal;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PermissionSuggestions {

    public static final String PLAYERS = SuggestionRegistry.PLAYERS;
    public static final String RANKS = "ranks";
    public static final String TAGS = "tags";
    public static final String USABLE_TAGS = "usable-tags";
    public static final String DECORATIVE_TAGS = "decorative-tags";
    public static final String MEDALS = "medals";
    public static final String USABLE_MEDALS = "usable-medals";
    public static final String TRACKS = "tracks";
    public static final String LIFETIMES = "lifetimes";
    public static final String NODES = "permission-nodes";
    public static final String NODE_OPTIONS = "permission-options";
    public static final List<String> VALUES = List.of("true", "false");

    private final Supplier<PermissionData> data;
    private final Function<UUID, Optional<ResolvedPermissions>> resolved;
    private final Supplier<Collection<String>> knownPermissions;
    private final Supplier<Collection<String>> serverGroups;
    private final Supplier<Collection<String>> worlds;

    public PermissionSuggestions(
        Supplier<PermissionData> data,
        Function<UUID, Optional<ResolvedPermissions>> resolved,
        Supplier<Collection<String>> knownPermissions,
        Supplier<Collection<String>> serverGroups,
        Supplier<Collection<String>> worlds
    ) {
        this.data = data;
        this.resolved = resolved;
        this.knownPermissions = knownPermissions;
        this.serverGroups = serverGroups;
        this.worlds = worlds;
    }

    public void register(Commands commands) {
        commands.suggestions(RANKS, sender -> ranks())
            .suggestions(TAGS, sender -> data.get().tags().keySet())
            .suggestions(USABLE_TAGS, this::usableTags)
            .suggestions(DECORATIVE_TAGS, sender -> decorativeTags())
            .suggestions(MEDALS, sender -> medals())
            .suggestions(USABLE_MEDALS, this::usableMedals)
            .suggestions(TRACKS, sender -> data.get().tracks().keySet())
            .suggestions(LIFETIMES, sender -> Lifetime.SUGGESTIONS)
            .suggestions(NODES, sender -> nodes())
            .suggestions(NODE_OPTIONS, sender -> options());
    }

    public List<String> ranks() {
        return data.get().groupsByWeight().stream().map(Group::id).toList();
    }

    public List<String> decorativeTags() {
        return data.get().tagsByWeight().stream().filter(Tag::decorative).map(Tag::id).toList();
    }

    public Collection<String> usableTags(CommandSender sender) {
        if (sender instanceof Player player && player.getUniqueId() != null) {
            return resolved.apply(player.getUniqueId()).map(found -> found.tags().stream().map(Tag::id).toList()).orElse(List.of());
        }
        return data.get().tags().keySet();
    }

    public List<String> medals() {
        return data.get().medalsByWeight().stream().map(Medal::id).toList();
    }

    public Collection<String> usableMedals(CommandSender sender) {
        if (sender instanceof Player player && player.getUniqueId() != null) {
            return resolved.apply(player.getUniqueId()).map(found -> found.medals().stream().map(Medal::id).toList()).orElse(List.of());
        }
        return medals();
    }

    public Collection<String> nodes() {
        TreeSet<String> nodes = new TreeSet<>(knownPermissions.get());
        data.get().groups().values().forEach(group -> group.nodes().forEach(node -> nodes.add(node.key())));
        data.get().tags().values().forEach(tag -> nodes.add(tag.permission()));
        data.get().medals().values().forEach(medal -> nodes.add(medal.permission()));
        data.get().groups().keySet().forEach(group -> nodes.add(PermissionNode.GROUP_PREFIX + group));
        return nodes;
    }

    public Collection<String> options() {
        TreeSet<String> options = new TreeSet<>(VALUES);
        options.addAll(Lifetime.SUGGESTIONS);
        serverGroups.get().forEach(group -> options.add(Contexts.SERVER + "=" + group));
        worlds.get().forEach(world -> options.add(Contexts.WORLD + "=" + world));
        return options;
    }
}

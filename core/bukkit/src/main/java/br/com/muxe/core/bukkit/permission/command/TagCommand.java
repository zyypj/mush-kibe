package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Lifetime;
import br.com.muxe.core.common.permission.domain.NodeGrant;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = TagCommand.NAME, aliases = "tags", description = "Lists and selects the tags a player can use.")
public final class TagCommand {

    public static final String NAME = "tag";
    public static final String MANAGE_PERMISSION = "muxe.tags.manage";

    private final PlayerRanks permissions;
    private final TargetLookup targets;
    private final Replies replies;
    private final TagList list;

    public TagCommand(PlayerRanks permissions, TargetLookup targets, Replies replies, TagList list) {
        this.permissions = permissions;
        this.targets = targets;
        this.replies = replies;
        this.list = list;
    }

    @Default
    public void tag(Player player, @Arg("tag") @Suggest(provider = PermissionSuggestions.USABLE_TAGS) Optional<String> id) {
        List<Tag> usable = permissions.resolved(player.getUniqueId()).map(ResolvedPermissions::tags).orElse(List.of());
        if (id.isEmpty()) {
            list.send(player, usable);
            return;
        }
        Optional<Tag> tag = permissions.data().tag(id.get().toLowerCase(Locale.ROOT));
        if (tag.isEmpty()) {
            replies.send(player, "tag-unknown", "tag", id.get());
            return;
        }
        if (!usable.contains(tag.get())) {
            replies.send(player, "tag-locked", "tag", display(tag.get()));
            return;
        }
        replies.then(player, permissions.sync().selectTag(player.getUniqueId(), Optional.of(tag.get().id())), result -> {
            if (result.isFailure()) {
                replies.send(player, Replies.FAILED);
            } else {
                replies.send(player, "tag-selected", "tag", display(tag.get()));
            }
        });
    }

    @Subcommand("dar")
    @Permission(MANAGE_PERMISSION)
    public void give(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("tag") @Suggest(provider = PermissionSuggestions.DECORATIVE_TAGS) String id,
        @Arg("tempo") @Suggest(provider = PermissionSuggestions.LIFETIMES) Optional<String> time
    ) {
        Optional<Tag> tag = decorative(sender, id);
        Optional<Lifetime> lifetime = time.map(Lifetime::parse).orElse(Optional.of(Lifetime.PERMANENT));
        if (tag.isEmpty()) {
            return;
        }
        if (lifetime.isEmpty()) {
            replies.send(sender, "rank-invalid-time", "input", time.orElse(""));
            return;
        }
        replies.then(sender, targets.find(name), target -> {
            if (target.isEmpty()) {
                replies.send(sender, "rank-player-unknown", "player", name);
                return;
            }
            replies.then(sender, permissions.sync().fetch(target.get().id()), user -> {
                Result<PermissionNode> grant = NodeGrant.grant(user, tag.get().permission(), Contexts.NONE, lifetime.get(), permissions.clock().instant());
                if (grant.isFailure()) {
                    replies.send(sender, "tag-already-held", "player", target.get().name(), "tag", display(tag.get()));
                    return;
                }
                replies.then(sender, permissions.sync().addNode(sender.getName(), target.get().id(), grant.orElseThrow()), result ->
                    replies.send(sender, result.isSuccess() ? "tag-given" : Replies.FAILED,
                        "player", target.get().name(), "tag", display(tag.get()), "time", RankText.lifetime(replies.messages(), lifetime.get())));
            });
        });
    }

    @Subcommand("remover")
    @Permission(MANAGE_PERMISSION)
    public void remove(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("tag") @Suggest(provider = PermissionSuggestions.DECORATIVE_TAGS) String id
    ) {
        Optional<Tag> tag = decorative(sender, id);
        if (tag.isEmpty()) {
            return;
        }
        replies.then(sender, targets.find(name), target -> {
            if (target.isEmpty()) {
                replies.send(sender, "rank-player-unknown", "player", name);
                return;
            }
            replies.then(sender, permissions.sync().removeNode(sender.getName(), target.get().id(), tag.get().permission(), Contexts.NONE), result -> {
                if (result.isSuccess()) {
                    replies.send(sender, "tag-removed", "player", target.get().name(), "tag", display(tag.get()));
                } else if (PermissionProblems.NODE_MISSING.equals(RankText.problem(result))) {
                    replies.send(sender, "tag-not-held", "player", target.get().name(), "tag", display(tag.get()));
                } else {
                    replies.send(sender, Replies.FAILED);
                }
            });
        });
    }

    @Usage
    public void usage(CommandSender sender) {
        replies.send(sender, sender.hasPermission(MANAGE_PERMISSION) ? "tag-usage-staff" : "tag-usage");
    }

    private Optional<Tag> decorative(CommandSender sender, String id) {
        Optional<Tag> tag = permissions.data().tag(id.toLowerCase(Locale.ROOT));
        if (tag.isEmpty()) {
            replies.send(sender, "tag-unknown", "tag", id);
            return Optional.empty();
        }
        if (!tag.get().decorative()) {
            replies.send(sender, "tag-not-decorative", "tag", display(tag.get()));
            return Optional.empty();
        }
        return tag;
    }

    private static String display(Tag tag) {
        return Colors.translate(tag.display());
    }
}

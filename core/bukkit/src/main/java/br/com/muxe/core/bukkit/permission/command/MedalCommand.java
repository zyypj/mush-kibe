package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Lifetime;
import br.com.muxe.core.common.permission.domain.Medal;
import br.com.muxe.core.common.permission.domain.NodeGrant;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = MedalCommand.NAME, aliases = "medalhas", description = "Lists and selects the medals a player can use.")
public final class MedalCommand {

    public static final String NAME = "medalha";
    public static final String NONE = "nenhuma";
    public static final String MANAGE_PERMISSION = "muxe.medals.manage";

    private final PlayerRanks permissions;
    private final TargetLookup targets;
    private final Replies replies;
    private final MedalList list;

    public MedalCommand(PlayerRanks permissions, TargetLookup targets, Replies replies, MedalList list) {
        this.permissions = permissions;
        this.targets = targets;
        this.replies = replies;
        this.list = list;
    }

    @Default
    public void medal(Player player, @Arg("medalha") @Suggest(value = NONE, provider = PermissionSuggestions.USABLE_MEDALS) Optional<String> id) {
        if (id.isEmpty()) {
            showList(player);
            return;
        }
        String chosen = id.get().toLowerCase(Locale.ROOT);
        if (NONE.equals(chosen)) {
            replies.then(player, permissions.sync().selectMedal(player.getUniqueId(), Optional.empty()), result ->
                replies.send(player, result.isSuccess() ? "medal-cleared" : Replies.FAILED));
            return;
        }
        Optional<Medal> medal = permissions.data().medal(chosen);
        if (medal.isEmpty()) {
            replies.send(player, "medal-unknown", "medal", id.get());
            return;
        }
        if (!usable(player).contains(medal.get())) {
            replies.send(player, "medal-locked", "medal", MedalList.label(medal.get()));
            return;
        }
        replies.then(player, permissions.sync().selectMedal(player.getUniqueId(), Optional.of(medal.get().id())), result ->
            replies.send(player, result.isSuccess() ? "medal-selected" : Replies.FAILED, "medal", MedalList.label(medal.get())));
    }

    @Subcommand("dar")
    @Permission(MANAGE_PERMISSION)
    public void give(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("medalha") @Suggest(provider = PermissionSuggestions.MEDALS) String id,
        @Arg("tempo") @Suggest(provider = PermissionSuggestions.LIFETIMES) Optional<String> time
    ) {
        Optional<Medal> medal = known(sender, id);
        Optional<Lifetime> lifetime = time.map(Lifetime::parse).orElse(Optional.of(Lifetime.PERMANENT));
        if (medal.isEmpty()) {
            return;
        }
        if (lifetime.isEmpty()) {
            replies.send(sender, "rank-invalid-time", "input", time.orElse(""));
            return;
        }
        String label = MedalList.label(medal.get());
        replies.then(sender, targets.find(name), target -> {
            if (target.isEmpty()) {
                replies.send(sender, "rank-player-unknown", "player", name);
                return;
            }
            replies.then(sender, permissions.sync().fetch(target.get().id()), user -> {
                Result<PermissionNode> grant = NodeGrant.grant(user, medal.get().permission(), Contexts.NONE, lifetime.get(), permissions.clock().instant());
                if (grant.isFailure()) {
                    replies.send(sender, "medal-already-held", "player", target.get().name(), "medal", label);
                    return;
                }
                replies.then(sender, permissions.sync().addNode(sender.getName(), target.get().id(), grant.orElseThrow()), result ->
                    replies.send(sender, result.isSuccess() ? "medal-given" : Replies.FAILED,
                        "player", target.get().name(), "medal", label, "time", RankText.lifetime(replies.messages(), lifetime.get())));
            });
        });
    }

    @Subcommand("remover")
    @Permission(MANAGE_PERMISSION)
    public void remove(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("medalha") @Suggest(provider = PermissionSuggestions.MEDALS) String id
    ) {
        Optional<Medal> medal = known(sender, id);
        if (medal.isEmpty()) {
            return;
        }
        String label = MedalList.label(medal.get());
        replies.then(sender, targets.find(name), target -> {
            if (target.isEmpty()) {
                replies.send(sender, "rank-player-unknown", "player", name);
                return;
            }
            replies.then(sender, permissions.sync().removeNode(sender.getName(), target.get().id(), medal.get().permission(), Contexts.NONE), result -> {
                if (result.isSuccess()) {
                    replies.send(sender, "medal-removed", "player", target.get().name(), "medal", label);
                } else if (PermissionProblems.NODE_MISSING.equals(RankText.problem(result))) {
                    replies.send(sender, "medal-not-held", "player", target.get().name(), "medal", label);
                } else {
                    replies.send(sender, Replies.FAILED);
                }
            });
        });
    }

    @Usage
    public void usage(CommandSender sender) {
        replies.send(sender, sender.hasPermission(MANAGE_PERMISSION) ? "medal-usage-staff" : "medal-usage");
    }

    private void showList(Player player) {
        list.send(player, usable(player), permissions.display(player.getUniqueId()));
    }

    private List<Medal> usable(Player player) {
        return permissions.resolved(player.getUniqueId()).map(ResolvedPermissions::medals).orElse(List.of());
    }

    private Optional<Medal> known(CommandSender sender, String id) {
        Optional<Medal> medal = permissions.data().medal(id.toLowerCase(Locale.ROOT));
        if (medal.isEmpty()) {
            replies.send(sender, "medal-unknown", "medal", id);
        }
        return medal;
    }
}

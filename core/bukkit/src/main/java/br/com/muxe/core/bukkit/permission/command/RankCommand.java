package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.bukkit.permission.command.TargetLookup.Target;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.Lifetime;
import br.com.muxe.core.common.permission.domain.NodeGrant;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Track;
import br.com.muxe.core.common.permission.domain.TrackMove;
import br.com.muxe.core.common.result.Result;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.command.CommandSender;

@Command(name = RankCommand.NAME, aliases = {"cargos", "rank"}, description = "Gives, removes and shows the ranks of players.", usage = "rank-usage")
@Permission(RankCommand.PERMISSION)
public final class RankCommand {

    public static final String NAME = "cargo";
    public static final String PERMISSION = "muxe.ranks.manage";

    private final PlayerRanks ranks;
    private final TargetLookup targets;
    private final Replies replies;
    private final RankReports reports;

    public RankCommand(PlayerRanks ranks, TargetLookup targets, Replies replies, RankReports reports) {
        this.ranks = ranks;
        this.targets = targets;
        this.replies = replies;
        this.reports = reports;
    }

    @Subcommand("ver")
    public void view(CommandSender sender, @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name) {
        withUser(sender, name, (target, user) -> reports.player(target.name(), user).forEach(sender::sendMessage));
    }

    @Subcommand("dar")
    public void give(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("cargo") @Suggest(provider = PermissionSuggestions.RANKS) String rankId,
        @Arg("tempo") @Suggest(provider = PermissionSuggestions.LIFETIMES) Optional<String> time
    ) {
        Optional<Group> group = rank(sender, rankId, false);
        Optional<Lifetime> lifetime = time.map(Lifetime::parse).orElse(Optional.of(Lifetime.PERMANENT));
        if (group.isEmpty()) {
            return;
        }
        if (lifetime.isEmpty()) {
            replies.send(sender, "rank-invalid-time", "input", time.orElse(""));
            return;
        }
        String display = display(group.get());
        withUser(sender, name, (target, user) -> {
            String key = PermissionNode.GROUP_PREFIX + group.get().id();
            Result<PermissionNode> grant = NodeGrant.grant(user, key, Contexts.NONE, lifetime.get(), ranks.clock().instant());
            if (grant.isFailure()) {
                replies.send(sender, "rank-already-held", "player", target.name(), "rank", display);
                return;
            }
            replies.then(sender, ranks.sync().addNode(sender.getName(), target.id(), grant.orElseThrow()), result ->
                replies.send(sender, result.isSuccess() ? "rank-given" : Replies.FAILED,
                    "player", target.name(), "rank", display, "time", RankText.lifetime(replies.messages(), lifetime.get())));
        });
    }

    @Subcommand("remover")
    public void remove(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("cargo") @Suggest(provider = PermissionSuggestions.RANKS) String rankId
    ) {
        Optional<Group> group = rank(sender, rankId, false);
        if (group.isEmpty()) {
            return;
        }
        String display = display(group.get());
        withUser(sender, name, (target, user) ->
            replies.then(sender, ranks.sync().removeNode(sender.getName(), target.id(), PermissionNode.GROUP_PREFIX + group.get().id(), Contexts.NONE), result -> {
                if (result.isSuccess()) {
                    replies.send(sender, "rank-removed", "player", target.name(), "rank", display);
                } else if (PermissionProblems.NODE_MISSING.equals(RankText.problem(result))) {
                    replies.send(sender, "rank-not-held", "player", target.name(), "rank", display);
                } else {
                    replies.send(sender, Replies.FAILED);
                }
            }));
    }

    @Subcommand(value = "promover", aliases = "promote")
    public void promote(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("trilha") @Suggest(provider = PermissionSuggestions.TRACKS) String trackId
    ) {
        track(sender, trackId).ifPresent(track -> withUser(sender, name, (target, user) ->
            move(sender, target, track, TrackMove.promote(track, user, ranks.clock().instant()), "rank-promoted")));
    }

    @Subcommand(value = "rebaixar", aliases = "demote")
    public void demote(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("trilha") @Suggest(provider = PermissionSuggestions.TRACKS) String trackId
    ) {
        track(sender, trackId).ifPresent(track -> withUser(sender, name, (target, user) ->
            move(sender, target, track, TrackMove.demote(track, user, ranks.clock().instant()), "rank-demoted")));
    }

    @Subcommand("listar")
    public void list(CommandSender sender) {
        reports.ranks().forEach(sender::sendMessage);
    }

    @Subcommand("info")
    public void info(CommandSender sender, @Arg("cargo") @Suggest(provider = PermissionSuggestions.RANKS) String rankId) {
        rank(sender, rankId, true).ifPresent(group -> reports.rank(group).forEach(sender::sendMessage));
    }

    private void move(CommandSender sender, Target target, Track track, Result<TrackMove> planned, String doneKey) {
        if (planned.isFailure()) {
            String key = TrackMove.TOP.equals(RankText.problem(planned)) ? "rank-track-top" : "rank-track-bottom";
            replies.send(sender, key, "player", target.name(), "track", track.id());
            return;
        }
        TrackMove move = planned.orElseThrow();
        PermissionData data = ranks.data();
        String reached = move.added().flatMap(data::group).or(() -> data.group(Group.DEFAULT)).map(this::display).orElse(Group.DEFAULT);
        Runnable add = () -> move.added().ifPresentOrElse(
            group -> replies.then(sender, ranks.sync().addNode(sender.getName(), target.id(),
                    PermissionNode.group(group, Optional.empty(), ranks.clock().instant())), result ->
                replies.send(sender, result.isSuccess() ? doneKey : Replies.FAILED, "player", target.name(), "rank", reached)),
            () -> replies.send(sender, doneKey, "player", target.name(), "rank", reached));
        move.removed().ifPresentOrElse(
            group -> replies.then(sender, ranks.sync().removeNode(sender.getName(), target.id(), PermissionNode.GROUP_PREFIX + group, Contexts.NONE), result -> {
                if (result.isSuccess()) {
                    add.run();
                } else {
                    replies.send(sender, Replies.FAILED);
                }
            }),
            add);
    }

    private void withUser(CommandSender sender, String name, BiConsumer<Target, PermissionUser> action) {
        replies.then(sender, targets.find(name), target -> {
            if (target.isEmpty()) {
                replies.send(sender, "rank-player-unknown", "player", name);
                return;
            }
            replies.then(sender, ranks.sync().fetch(target.get().id()), user -> action.accept(target.get(), user));
        });
    }

    private Optional<Group> rank(CommandSender sender, String id, boolean allowDefault) {
        Optional<Group> group = ranks.data().group(id.toLowerCase(Locale.ROOT));
        if (group.isEmpty()) {
            replies.send(sender, "rank-unknown", "rank", id);
        } else if (!allowDefault && group.get().id().equals(Group.DEFAULT)) {
            replies.send(sender, "rank-default", "rank", display(group.get()));
            return Optional.empty();
        }
        return group;
    }

    private Optional<Track> track(CommandSender sender, String id) {
        Optional<Track> track = ranks.data().track(id.toLowerCase(Locale.ROOT));
        if (track.isEmpty()) {
            replies.send(sender, "rank-track-unknown", "track", id);
        }
        return track;
    }

    private String display(Group group) {
        return NodeText.group(ranks.data(), group);
    }
}

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
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionResolver;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.domain.Tristate;
import br.com.muxe.core.common.result.Result;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.command.CommandSender;

@Command(name = PermissionCommand.NAME, aliases = {"permissoes", "perm"}, description = "Sets and removes permissions of players and ranks.", usage = "perm-usage")
@Permission(PermissionCommand.PERMISSION)
public final class PermissionCommand {

    public static final String NAME = "permissao";
    public static final String PERMISSION = "muxe.permissions.manage";

    private final PlayerRanks ranks;
    private final TargetLookup targets;
    private final Replies replies;
    private final RankReports reports;
    private final NodeText text;

    public PermissionCommand(PlayerRanks ranks, TargetLookup targets, Replies replies, RankReports reports) {
        this.ranks = ranks;
        this.targets = targets;
        this.replies = replies;
        this.reports = reports;
        this.text = new NodeText(replies.messages());
    }

    @Subcommand("jogador ver")
    public void userView(CommandSender sender, @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name) {
        withUser(sender, name, (target, user) -> reports.userNodes(target.name(), user).forEach(sender::sendMessage));
    }

    @Subcommand("jogador definir")
    public void userSet(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("permissão") @Suggest(provider = PermissionSuggestions.NODES) String key,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> first,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> second,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> third,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> fourth
    ) {
        node(sender, key, List.of(first, second, third, fourth)).ifPresent(node -> withUser(sender, name, (target, user) ->
            replies.then(sender, ranks.sync().addNode(sender.getName(), target.id(), node), result ->
                replies.send(sender, result.isSuccess() ? "perm-user-set" : Replies.FAILED, placeholders(node, "player", target.name())))));
    }

    @Subcommand("jogador remover")
    public void userRemove(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("permissão") @Suggest(provider = PermissionSuggestions.NODES) String key,
        @Arg("contexto") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> first,
        @Arg("contexto") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> second
    ) {
        String clean = key.toLowerCase(Locale.ROOT);
        contexts(sender, List.of(first, second)).ifPresent(contexts -> withUser(sender, name, (target, user) ->
            replies.then(sender, ranks.sync().removeNode(sender.getName(), target.id(), clean, contexts), result -> {
                String reply = result.isSuccess() ? "perm-user-removed"
                    : PermissionProblems.NODE_MISSING.equals(RankText.problem(result)) ? "perm-user-missing" : Replies.FAILED;
                replies.send(sender, reply, "player", target.name(), "key", clean, "contexts", text.contexts(contexts));
            })));
    }

    @Subcommand("jogador testar")
    public void userTest(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("permissão") @Suggest(provider = PermissionSuggestions.NODES) String key
    ) {
        withUser(sender, name, (target, user) -> {
            Optional<ResolvedPermissions> online = ranks.resolved(target.id());
            ResolvedPermissions resolved = online.orElseGet(() -> PermissionResolver.resolve(ranks.data(), user, Contexts.NONE, ranks.clock().instant()));
            Tristate result = resolved.check(key);
            String answer = switch (result) {
                case TRUE -> "perm-test-true";
                case FALSE -> "perm-test-false";
                case UNDEFINED -> "perm-test-undefined";
            };
            replies.send(sender, "perm-test", "player", target.name(), "key", key.toLowerCase(Locale.ROOT), "result", replies.messages().get(answer));
            replies.send(sender, online.isPresent() ? "perm-test-online" : "perm-test-offline");
        });
    }

    @Subcommand("cargo ver")
    public void groupView(CommandSender sender, @Arg("cargo") @Suggest(provider = PermissionSuggestions.RANKS) String rankId) {
        group(sender, rankId).ifPresent(group -> reports.rank(group).forEach(sender::sendMessage));
    }

    @Subcommand("cargo definir")
    public void groupSet(
        CommandSender sender,
        @Arg("cargo") @Suggest(provider = PermissionSuggestions.RANKS) String rankId,
        @Arg("permissão") @Suggest(provider = PermissionSuggestions.NODES) String key,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> first,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> second,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> third,
        @Arg("opções") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> fourth
    ) {
        group(sender, rankId).ifPresent(group -> node(sender, key, List.of(first, second, third, fourth)).ifPresent(node ->
            saveGroup(sender, group.with(node), "perm-group-set", placeholders(node, "rank", NodeText.group(ranks.data(), group)))));
    }

    @Subcommand("cargo remover")
    public void groupRemove(
        CommandSender sender,
        @Arg("cargo") @Suggest(provider = PermissionSuggestions.RANKS) String rankId,
        @Arg("permissão") @Suggest(provider = PermissionSuggestions.NODES) String key,
        @Arg("contexto") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> first,
        @Arg("contexto") @Suggest(provider = PermissionSuggestions.NODE_OPTIONS) Optional<String> second
    ) {
        String clean = key.toLowerCase(Locale.ROOT);
        group(sender, rankId).ifPresent(group -> contexts(sender, List.of(first, second)).ifPresent(contexts -> {
            String[] placeholders = {"rank", NodeText.group(ranks.data(), group), "key", clean, "contexts", text.contexts(contexts)};
            group.without(clean, contexts).ifPresentOrElse(
                updated -> saveGroup(sender, updated, "perm-group-removed", placeholders),
                () -> replies.send(sender, "perm-group-missing", placeholders));
        }));
    }

    private void saveGroup(CommandSender sender, Group updated, String doneKey, String[] placeholders) {
        replies.then(sender, ranks.sync().apply(sender.getName(), List.of(new PermissionChange.PutGroup(updated))), result -> {
            if (result instanceof Result.Failure<Void> failure) {
                replies.send(sender, "perm-group-refused", "reason", failure.problem().message());
            } else {
                replies.send(sender, doneKey, placeholders);
            }
        });
    }

    private Optional<PermissionNode> node(CommandSender sender, String key, List<Optional<String>> words) {
        NodeOptions.Parsed parsed = NodeOptions.parse(words);
        if (parsed.options().isEmpty()) {
            replies.send(sender, "perm-invalid-option", "input", parsed.rejected().orElse(""));
            return Optional.empty();
        }
        NodeOptions options = parsed.options().get();
        Instant now = ranks.clock().instant();
        try {
            return Optional.of(new PermissionNode(key, options.value(), options.contexts(), options.lifetime().expiryAfter(Optional.empty(), now), now, now));
        } catch (IllegalArgumentException invalid) {
            replies.send(sender, "perm-invalid-node", "input", key);
            return Optional.empty();
        }
    }

    private Optional<Contexts> contexts(CommandSender sender, List<Optional<String>> words) {
        Optional<Contexts> contexts = NodeOptions.contexts(words);
        if (contexts.isEmpty()) {
            replies.send(sender, "perm-invalid-option", "input", String.join(" ", words.stream().flatMap(Optional::stream).toList()));
        }
        return contexts;
    }

    private Optional<Group> group(CommandSender sender, String id) {
        Optional<Group> group = ranks.data().group(id.toLowerCase(Locale.ROOT));
        if (group.isEmpty()) {
            replies.send(sender, "rank-unknown", "rank", id);
        }
        return group;
    }

    private String[] placeholders(PermissionNode node, String name, String value) {
        return new String[] {
            name, value,
            "key", node.key(),
            "value", text.value(node.value()),
            "contexts", text.contexts(node.contexts()),
            "time", text.time(node.expiresAt(), ranks.clock().instant())
        };
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
}

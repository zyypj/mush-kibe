package br.com.muxe.core.bukkit.credit.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.credit.CreditFormat;
import br.com.muxe.core.bukkit.permission.command.PermissionSuggestions;
import br.com.muxe.core.bukkit.permission.command.Replies;
import br.com.muxe.core.bukkit.permission.command.TargetLookup;
import br.com.muxe.core.bukkit.permission.command.TargetLookup.Target;
import br.com.muxe.core.common.credit.application.command.ChangeCreditsCommand;
import br.com.muxe.core.common.credit.application.query.FindCreditsQuery;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import java.util.function.Consumer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = CreditCommand.NAME, aliases = {"credito", "credits"}, description = "Shows and changes credit balances.")
public final class CreditCommand {

    public static final String NAME = "creditos";
    public static final String ADMIN_PERMISSION = "muxe.credits.admin";
    public static final String REASON = "staff";
    public static final long MAX_AMOUNT = 1_000_000_000L;

    private final CommandBus commands;
    private final QueryBus queries;
    private final TargetLookup targets;
    private final Replies replies;

    public CreditCommand(CommandBus commands, QueryBus queries, TargetLookup targets, Replies replies) {
        this.commands = commands;
        this.queries = queries;
        this.targets = targets;
        this.replies = replies;
    }

    @Default
    public void own(Player player) {
        replies.then(player, queries.ask(new FindCreditsQuery(player.getUniqueId())), balance ->
            replies.send(player, "credits-own", "amount", CreditFormat.format(balance)));
    }

    @Subcommand("ver")
    @Permission(ADMIN_PERMISSION)
    public void show(CommandSender sender, @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name) {
        withTarget(sender, name, target -> replies.then(sender, queries.ask(new FindCreditsQuery(target.id())), balance ->
            replies.send(sender, "credits-of", "player", target.name(), "amount", CreditFormat.format(balance))));
    }

    @Subcommand("dar")
    @Permission(ADMIN_PERMISSION)
    public void give(CommandSender sender, @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name, @Arg("quantia") long amount) {
        if (valid(sender, amount)) {
            change(sender, name, amount, "credits-given");
        }
    }

    @Subcommand("remover")
    @Permission(ADMIN_PERMISSION)
    public void take(CommandSender sender, @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name, @Arg("quantia") long amount) {
        if (valid(sender, amount)) {
            change(sender, name, -amount, "credits-taken");
        }
    }

    private boolean valid(CommandSender sender, long amount) {
        if (amount <= 0 || amount > MAX_AMOUNT) {
            replies.send(sender, "credits-invalid-amount", "max", CreditFormat.format(MAX_AMOUNT));
            return false;
        }
        return true;
    }

    private void change(CommandSender sender, String name, long amount, String done) {
        withTarget(sender, name, target -> replies.then(sender, commands.dispatch(new ChangeCreditsCommand(target.id(), amount, REASON)), result -> {
            if (result.isFailure()) {
                replies.send(sender, "credits-not-enough", "player", target.name());
                return;
            }
            replies.send(sender, done, "player", target.name(), "amount", CreditFormat.format(Math.abs(amount)), "balance", CreditFormat.format(result.orElseThrow()));
        }));
    }

    private void withTarget(CommandSender sender, String name, Consumer<Target> action) {
        replies.then(sender, targets.find(name), target -> {
            if (target.isEmpty()) {
                replies.send(sender, "rank-player-unknown", "player", name);
                return;
            }
            action.accept(target.get());
        });
    }
}

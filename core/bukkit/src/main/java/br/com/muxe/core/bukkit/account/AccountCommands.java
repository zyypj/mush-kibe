package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import br.com.muxe.core.bukkit.permission.command.Replies;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsQuery;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AccountCommands {

    public static final String OTHERS_PERMISSION = "muxe.account.others";
    public static final String STAFF_PERMISSION = "muxe.account.staff";

    private final AccountProfiles profiles;
    private final AccountLines lines;
    private final AccountMenu menu;
    private final Replies replies;

    public AccountCommands(AccountProfiles profiles, AccountLines lines, AccountMenu menu, Replies replies) {
        this.profiles = profiles;
        this.lines = lines;
        this.menu = menu;
        this.replies = replies;
    }

    @Command(name = "account", aliases = "conta", description = "Shows the account details of a player.")
    public void account(CommandSender sender, @Arg("jogador") @Suggest(provider = SuggestionRegistry.PLAYERS) Optional<String> name) {
        query(sender, name).ifPresent(query -> replies.then(sender, profiles.find(query, name.orElse(sender.getName())), profile -> {
            if (profile.isEmpty()) {
                replies.send(sender, "account.not-found", "player", name.orElse(sender.getName()));
            } else {
                lines.lines(profile.get()).forEach(sender::sendMessage);
            }
        }));
    }

    @Command(name = "accountmenu", aliases = "contamenu", description = "Opens the account menu of a player.")
    public void accountMenu(Player player, @Arg("jogador") @Suggest(provider = SuggestionRegistry.PLAYERS) Optional<String> name) {
        query(player, name).ifPresent(query -> replies.then(player, profiles.find(query, name.orElse(player.getName())), profile -> {
            if (profile.isEmpty()) {
                replies.send(player, "account.not-found", "player", name.orElse(player.getName()));
            } else {
                menu.menu(profile.get(), player.hasPermission(STAFF_PERMISSION)).open(player);
            }
        }));
    }

    private Optional<FindAccountDetailsQuery> query(CommandSender sender, Optional<String> name) {
        boolean self = name.isEmpty() || name.get().equalsIgnoreCase(sender.getName());
        if (self && !(sender instanceof Player)) {
            replies.send(sender, "account.usage");
            return Optional.empty();
        }
        if (!self && !sender.hasPermission(OTHERS_PERMISSION)) {
            replies.send(sender, CommandMessages.NO_PERMISSION);
            return Optional.empty();
        }
        if (self) {
            return Optional.of(FindAccountDetailsQuery.byId(((Player) sender).getUniqueId()));
        }
        return Optional.of(FindAccountDetailsQuery.byUsername(name.get()));
    }
}

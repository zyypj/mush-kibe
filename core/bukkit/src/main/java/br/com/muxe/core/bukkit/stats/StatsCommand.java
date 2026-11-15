package br.com.muxe.core.bukkit.stats;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.account.application.query.FindAccountByUsernameQuery;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;

public final class StatsCommand {

    public static final String NOT_FOUND = "statistics-not-found";

    private final StatsMenus menus;
    private final QueryBus queries;
    private final Executor mainThread;
    private final MessageCatalog messages;

    public StatsCommand(StatsMenus menus, QueryBus queries, Executor mainThread, MessageCatalog messages) {
        this.menus = menus;
        this.queries = queries;
        this.mainThread = mainThread;
        this.messages = messages;
    }

    @Command(name = "stats", aliases = "estatisticas", description = "Opens the statistics of a player.")
    public void stats(Player player, @Arg("jogador") @Suggest(provider = SuggestionRegistry.PLAYERS) Optional<String> name) {
        if (name.isEmpty() || name.get().equalsIgnoreCase(player.getName())) {
            menus.openHub(player, new Target(player.getUniqueId(), player.getName(), true), Optional.empty());
            return;
        }
        Player online = player.getServer().getPlayerExact(name.get());
        if (online != null) {
            menus.openHub(player, new Target(online.getUniqueId(), online.getName(), false), Optional.empty());
            return;
        }
        queries.ask(new FindAccountByUsernameQuery(name.get())).whenComplete((found, error) -> mainThread.execute(() -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null || found.isEmpty()) {
                player.sendMessage(messages.get(player, NOT_FOUND, "player", name.get()));
                return;
            }
            menus.openHub(player, new Target(found.get().id(), found.get().username(), false), Optional.empty());
        }));
    }
}

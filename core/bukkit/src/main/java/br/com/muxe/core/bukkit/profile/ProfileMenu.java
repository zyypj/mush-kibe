package br.com.muxe.core.bukkit.profile;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.button.ButtonActions;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.common.account.application.query.FindAccountQuery;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class ProfileMenu {

    private final ProfileSettings settings;
    private final ButtonActions actions;
    private final QueryBus queries;
    private final Executor mainThread;
    private final Logger logger;

    public ProfileMenu(ProfileSettings settings, ButtonActions actions, QueryBus queries, Executor mainThread, Logger logger) {
        this.settings = settings;
        this.actions = actions;
        this.queries = queries;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    public void open(Player player) {
        queries.ask(new FindAccountQuery(player.getUniqueId())).whenComplete((account, error) -> mainThread.execute(() -> {
            Optional<Instant> firstSeenAt = Optional.empty();
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the account of " + player.getName() + ": " + error.getMessage());
            } else {
                firstSeenAt = account.map(Account::firstSeenAt);
            }
            if (player.isOnline()) {
                menu(player.getName(), firstSeenAt).open(player);
            }
        }));
    }

    public Menu menu(String playerName, Optional<Instant> firstSeenAt) {
        Menu.Builder builder = Menu.builder(settings.title()).rows(settings.rows());
        ConfiguredButton head = settings.head();
        builder.button(head.slot(), head.item().skullOwner(playerName).replacing(
            "player", playerName,
            "first-login", settings.firstLogin(firstSeenAt),
            "credits", Long.toString(settings.credits())
        ), context -> actions.run(context, head));
        for (ConfiguredButton button : settings.buttons()) {
            builder.button(button.slot(), button.item(), context -> actions.run(context, button));
        }
        return builder.build();
    }
}

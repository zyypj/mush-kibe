package br.com.muxe.core.bukkit.stats;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.stats.application.query.FindBedWarsTotalsQuery;
import br.com.muxe.core.common.stats.domain.BedWarsTotals;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class StatsMenus {

    public static final String SOON = "statistics-soon";

    private final Localized<StatsSettings> settings;
    private final MessageCatalog messages;
    private final QueryBus queries;
    private final Executor mainThread;
    private final Logger logger;

    public StatsMenus(Localized<StatsSettings> settings, MessageCatalog messages, QueryBus queries, Executor mainThread, Logger logger) {
        this.settings = settings;
        this.messages = messages;
        this.queries = queries;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    public void openHub(Player viewer, Target target, Optional<Consumer<Player>> back) {
        StatsSettings.Hub hub = settings.in(messages.language(viewer)).hub();
        Menu.Builder menu = Menu.builder(hub.title(target)).rows(hub.rows());
        for (StatsPages.Shown shown : StatsPages.hub(hub, back.isPresent())) {
            menu.button(shown.slot(), shown.icon(), context -> {
                switch (shown.click()) {
                    case NOTHING -> {
                    }
                    case SOON -> context.player().sendMessage(messages.get(context.player(), SOON));
                    case BEDWARS -> openBedWars(context.player(), target, Optional.of(player -> openHub(player, target, back)));
                    case BACK -> back.ifPresent(open -> open.accept(context.player()));
                }
            });
        }
        menu.build().open(viewer);
    }

    public void openBedWars(Player viewer, Target target, Optional<Consumer<Player>> back) {
        queries.ask(new FindBedWarsTotalsQuery(target.id())).whenComplete((totals, error) -> mainThread.execute(() -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the Bed Wars statistics of " + target.name() + ": " + error.getMessage());
            }
            if (viewer.isOnline()) {
                showBedWars(viewer, target, error == null ? totals : BedWarsTotals.EMPTY, back);
            }
        }));
    }

    private void showBedWars(Player viewer, Target target, BedWarsTotals totals, Optional<Consumer<Player>> back) {
        StatsSettings current = settings.in(messages.language(viewer));
        StatsSettings.BedWars page = current.bedwars();
        Menu.Builder menu = Menu.builder(page.title(target)).rows(page.rows());
        List<StatsPages.Shown> shown = StatsPages.bedwars(page, totals, back.isPresent(), current.hub().title(target));
        for (StatsPages.Shown entry : shown) {
            menu.button(entry.slot(), entry.icon(), context -> {
                if (entry.click() == StatsPages.Click.BACK) {
                    back.ifPresent(open -> open.accept(context.player()));
                }
            });
        }
        menu.build().open(viewer);
    }
}

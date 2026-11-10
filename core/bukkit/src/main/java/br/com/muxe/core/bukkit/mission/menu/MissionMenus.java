package br.com.muxe.core.bukkit.mission.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.mission.config.MissionConfig;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.mission.application.query.FindMissionsQuery;
import br.com.muxe.core.common.mission.domain.MissionBoard;
import br.com.muxe.core.common.mission.domain.MissionCadence;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class MissionMenus {

    private final String game;
    private final Localized<MissionConfig> configs;
    private final LanguageLookup languages;
    private final QueryBus queries;
    private final Executor mainThread;
    private final Clock clock;
    private final ZoneId zone;
    private final Logger logger;

    public MissionMenus(String game, MissionConfig config, QueryBus queries, Executor mainThread, Clock clock, ZoneId zone, Logger logger) {
        this(game, Localized.same(config), LanguageLookup.DEFAULT, queries, mainThread, clock, zone, logger);
    }

    public MissionMenus(String game, Localized<MissionConfig> configs, LanguageLookup languages, QueryBus queries, Executor mainThread, Clock clock, ZoneId zone,
                        Logger logger) {
        this.game = game;
        this.configs = configs;
        this.languages = languages;
        this.queries = queries;
        this.mainThread = mainThread;
        this.clock = clock;
        this.zone = zone;
        this.logger = logger;
    }

    public void open(Player player, Optional<Consumer<Player>> back) {
        MissionConfig config = configs.in(languages.of(player.getUniqueId()));
        if (config.menu() == null) {
            return;
        }
        queries.ask(new FindMissionsQuery(game, player.getUniqueId())).whenCompleteAsync((board, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the missions of " + player.getName(), error);
                return;
            }
            if (player.isOnline()) {
                menu(config, board, back).open(player);
            }
        }, mainThread);
    }

    private Menu menu(MissionConfig config, MissionBoard board, Optional<Consumer<Player>> back) {
        MissionConfig.Menu menu = config.menu();
        Menu.Builder builder = Menu.builder(menu.title()).rows(menu.rows());
        for (Map.Entry<MissionCadence, MissionConfig.Category> category : menu.categories().entrySet()) {
            MissionCadence cadence = category.getKey();
            Duration remaining = Duration.between(clock.instant(), cadence.end(clock.instant(), zone));
            builder.button(category.getValue().slot(), category.getValue().item().lore(MissionView.categoryLore(menu, board.completed(cadence), board.count(cadence), remaining)));
            int slot = category.getValue().slot() + 1;
            for (MissionBoard.Entry entry : board.entries()) {
                if (entry.mission().cadence() != cadence) {
                    continue;
                }
                MissionConfig.Texts texts = config.texts(entry.mission().id());
                ItemBuilder icon = menu.mission().name(menu.mission().name().replace("{name}", texts.name()))
                    .lore(MissionView.missionLore(menu, texts, entry, category.getValue().label()))
                    .glow(!entry.progress().completed());
                builder.button(slot++, icon);
            }
        }
        back.ifPresent(target -> builder.button(menu.backSlot(), menu.back(), context -> target.accept(context.player())));
        return builder.build();
    }
}

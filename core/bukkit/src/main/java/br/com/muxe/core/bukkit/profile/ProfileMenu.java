package br.com.muxe.core.bukkit.profile;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.button.ButtonActions;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.bukkit.permission.PlayerRanks;
import br.com.muxe.core.common.account.application.query.AccountProfileView;
import br.com.muxe.core.common.account.application.query.FindAccountQuery;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public final class ProfileMenu {

    private final Localized<ProfileSettings> settings;
    private final LanguageLookup languages;
    private final ButtonActions actions;
    private final QueryBus queries;
    private final PlayerRanks ranks;
    private final Executor mainThread;
    private final Logger logger;

    public ProfileMenu(ProfileSettings settings, ButtonActions actions, QueryBus queries, PlayerRanks ranks, Executor mainThread, Logger logger) {
        this(Localized.same(settings), LanguageLookup.DEFAULT, actions, queries, ranks, mainThread, logger);
    }

    public ProfileMenu(Localized<ProfileSettings> settings, LanguageLookup languages, ButtonActions actions, QueryBus queries, PlayerRanks ranks, Executor mainThread,
                       Logger logger) {
        this.settings = settings;
        this.languages = languages;
        this.actions = actions;
        this.queries = queries;
        this.ranks = ranks;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    public void open(Player player) {
        queries.ask(new FindAccountQuery(player.getUniqueId())).whenComplete((account, error) -> mainThread.execute(() -> {
            Optional<Instant> firstSeenAt = Optional.empty();
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the account of " + player.getName() + ": " + error.getMessage());
            } else {
                firstSeenAt = account.map(AccountProfileView::firstSeenAt);
            }
            if (player.isOnline()) {
                menu(languages.of(player.getUniqueId()), ProfileHead.of(ranks, player.getUniqueId(), player.getName(), firstSeenAt)).open(player);
            }
        }));
    }

    public Menu menu(ProfileHead profile) {
        return menu(Language.DEFAULT, profile);
    }

    public Menu menu(Language language, ProfileHead profile) {
        ProfileSettings settings = this.settings.in(language);
        Menu.Builder builder = Menu.builder(settings.title()).rows(settings.rows());
        ConfiguredButton head = settings.head();
        ItemBuilder icon = head.item().skullOwner(profile.player()).replacing(
            "player", profile.player(),
            "name", profile.displayName(),
            "first-login", settings.firstLogin(profile.firstSeenAt()),
            "credits", Long.toString(settings.credits())
        );
        Instant now = ranks.clock().instant();
        for (String line : settings.rankLines(profile.ranks(), now)) {
            icon = icon.addLore(line);
        }
        builder.button(head.slot(), icon, context -> actions.run(context, head));
        for (ConfiguredButton button : settings.buttons()) {
            builder.button(button.slot(), button.item(), context -> actions.run(context, button));
        }
        return builder.build();
    }
}

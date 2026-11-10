package br.com.muxe.core.bukkit.selector.game;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.button.ButtonActions;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import org.bukkit.entity.Player;

public final class GameSelectorMenu {

    private final Localized<GameSelectorSettings> settings;
    private final ButtonActions actions;
    private final LanguageLookup languages;

    public GameSelectorMenu(GameSelectorSettings settings, ButtonActions actions) {
        this(Localized.same(settings), actions, LanguageLookup.DEFAULT);
    }

    public GameSelectorMenu(Localized<GameSelectorSettings> settings, ButtonActions actions, LanguageLookup languages) {
        this.settings = settings;
        this.actions = actions;
        this.languages = languages;
    }

    public void open(Player player) {
        menu(languages.of(player.getUniqueId())).open(player);
    }

    public Menu menu() {
        return menu(Language.DEFAULT);
    }

    public Menu menu(Language language) {
        GameSelectorSettings current = settings.in(language);
        Menu.Builder builder = Menu.builder(current.title()).rows(current.rows());
        for (ConfiguredButton button : current.buttons()) {
            String players = Integer.toString(actions.players(button.action()));
            builder.button(button.slot(), button.item().replacing("players", players), context -> actions.run(context, button));
        }
        return builder.build();
    }
}

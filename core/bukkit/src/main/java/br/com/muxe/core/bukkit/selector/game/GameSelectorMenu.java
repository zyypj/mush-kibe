package br.com.muxe.core.bukkit.selector.game;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.button.ButtonActions;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import org.bukkit.entity.Player;

public final class GameSelectorMenu {

    private final GameSelectorSettings settings;
    private final ButtonActions actions;

    public GameSelectorMenu(GameSelectorSettings settings, ButtonActions actions) {
        this.settings = settings;
        this.actions = actions;
    }

    public void open(Player player) {
        menu().open(player);
    }

    public Menu menu() {
        Menu.Builder builder = Menu.builder(settings.title()).rows(settings.rows());
        for (ConfiguredButton button : settings.buttons()) {
            String players = Integer.toString(actions.players(button.action()));
            builder.button(button.slot(), button.item().replacing("players", players), context -> actions.run(context, button));
        }
        return builder.build();
    }
}

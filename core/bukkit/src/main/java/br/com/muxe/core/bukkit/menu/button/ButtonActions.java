package br.com.muxe.core.bukkit.menu.button;

import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.network.GameConnector;
import br.com.muxe.core.bukkit.shop.ShopAccess;

public final class ButtonActions {

    private final GameConnector games;
    private final ShopAccess shop;

    public ButtonActions(GameConnector games, ShopAccess shop) {
        this.games = games;
        this.shop = shop;
    }

    public int players(ButtonAction action) {
        return action.type() == ButtonAction.Type.GAME ? games.players(action.target()) : 0;
    }

    public void run(ClickContext context, ConfiguredButton button) {
        ButtonAction action = button.action();
        switch (action.type()) {
            case NONE -> {
            }
            case GAME -> {
                context.close();
                games.connect(context.player(), action.target(), button.displayName());
            }
            case SHOP -> shop.open(context.player(), action.target());
        }
    }
}

package br.com.muxe.core.bukkit.hotbar;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public record HotbarActions(
    Consumer<Player> gameSelector,
    Consumer<Player> profile,
    Consumer<Player> cosmetics,
    Consumer<Player> visibility,
    Consumer<Player> lobbySelector
) {

    public static final Consumer<Player> NOTHING = player -> {
    };

    public HotbarActions {
        Objects.requireNonNull(gameSelector, "gameSelector");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(cosmetics, "cosmetics");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(lobbySelector, "lobbySelector");
    }

    public void run(HotbarAction action, Player player) {
        switch (action) {
            case GAME_SELECTOR -> gameSelector.accept(player);
            case PROFILE -> profile.accept(player);
            case COSMETICS -> cosmetics.accept(player);
            case VISIBILITY -> visibility.accept(player);
            case LOBBY_SELECTOR -> lobbySelector.accept(player);
        }
    }
}

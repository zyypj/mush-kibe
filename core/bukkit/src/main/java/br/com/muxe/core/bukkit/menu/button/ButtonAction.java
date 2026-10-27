package br.com.muxe.core.bukkit.menu.button;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record ButtonAction(Type type, String target) {

    public static final ButtonAction NONE = new ButtonAction(Type.NONE, "");

    public ButtonAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        if (type == Type.GAME && target.isBlank()) {
            throw new IllegalArgumentException("A game button needs the group of its servers");
        }
    }

    public static ButtonAction game(String group) {
        return new ButtonAction(Type.GAME, group.toLowerCase(Locale.ROOT));
    }

    public static ButtonAction shop(String category) {
        return new ButtonAction(Type.SHOP, category);
    }

    public static ButtonAction from(ConfigurationSection section) {
        String game = section.getString("game", "");
        if (!game.isBlank()) {
            return game(game);
        }
        if (section.isSet("shop")) {
            return shop(section.getString("shop", ""));
        }
        return NONE;
    }

    public enum Type {
        NONE,
        GAME,
        SHOP
    }
}

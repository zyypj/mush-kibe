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
        if (type == Type.COMMAND && target.isBlank()) {
            throw new IllegalArgumentException("A command button needs the command it runs");
        }
    }

    public static ButtonAction game(String group) {
        return new ButtonAction(Type.GAME, group.toLowerCase(Locale.ROOT));
    }

    public static ButtonAction shop(String category) {
        return new ButtonAction(Type.SHOP, category);
    }

    public static ButtonAction command(String line) {
        String trimmed = line.strip();
        return new ButtonAction(Type.COMMAND, trimmed.startsWith("/") ? trimmed.substring(1) : trimmed);
    }

    public static ButtonAction from(ConfigurationSection section) {
        String game = section.getString("game", "");
        if (!game.isBlank()) {
            return game(game);
        }
        if (section.isSet("shop")) {
            return shop(section.getString("shop", ""));
        }
        if (section.isSet("command")) {
            return command(section.getString("command", ""));
        }
        return NONE;
    }

    public enum Type {
        NONE,
        GAME,
        SHOP,
        COMMAND
    }
}

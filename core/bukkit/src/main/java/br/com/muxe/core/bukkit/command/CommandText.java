package br.com.muxe.core.bukkit.command;

import java.util.Locale;
import java.util.OptionalInt;

public final class CommandText {

    public static final String EMPTY_LINE = "{vazio}";

    private CommandText() {
    }

    public static String line(String text) {
        return text.equalsIgnoreCase(EMPTY_LINE) ? "" : text;
    }

    public static String id(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    public static OptionalInt number(String value) {
        try {
            int number = Integer.parseInt(value);
            return number > 0 ? OptionalInt.of(number) : OptionalInt.empty();
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }
}

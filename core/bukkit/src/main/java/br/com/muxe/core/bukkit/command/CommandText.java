package br.com.muxe.core.bukkit.command;

import java.util.Arrays;
import java.util.Locale;
import java.util.OptionalInt;

public final class CommandText {

    public static final String EMPTY_LINE = "{vazio}";

    private CommandText() {
    }

    public static String join(String[] args, int from) {
        if (from >= args.length) {
            return "";
        }
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    public static String line(String[] args, int from) {
        String text = join(args, from);
        return text.equalsIgnoreCase(EMPTY_LINE) ? "" : text;
    }

    public static String word(String[] args, int index) {
        return index < args.length ? args[index].toLowerCase(Locale.ROOT) : "";
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

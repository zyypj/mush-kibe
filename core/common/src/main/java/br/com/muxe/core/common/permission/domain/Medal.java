package br.com.muxe.core.common.permission.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Medal(String id, String name, String symbol, String color, int weight) {

    public static final String PERMISSION_PREFIX = "muxe.medal.";

    private static final Pattern COLOR = Pattern.compile("&[0-9a-f]");

    public Medal {
        PermissionIds.require(id);
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > 48) {
            throw new IllegalArgumentException("A medal name has 1 to 48 characters, got: " + name);
        }
        Objects.requireNonNull(symbol, "symbol");
        if (symbol.isBlank() || symbol.codePointCount(0, symbol.length()) > 4) {
            throw new IllegalArgumentException("A medal symbol has 1 to 4 characters, got: " + symbol);
        }
        color = Objects.requireNonNull(color, "color").toLowerCase(Locale.ROOT);
        if (!COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("A medal color is one & code like &a, got: " + color);
        }
    }

    public String display() {
        return color + symbol;
    }

    public String chatPrefix() {
        return display() + " ";
    }

    public String permission() {
        return PERMISSION_PREFIX + id;
    }
}

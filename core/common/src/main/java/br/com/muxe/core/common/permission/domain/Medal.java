package br.com.muxe.core.common.permission.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Medal(String id, String name, String symbol, String color, int weight) {

    public static final String PERMISSION_PREFIX = "muxe.medal.";

    public static final int MAX_SYMBOL_LENGTH = 32;

    private static final Pattern COLOR = Pattern.compile("&[0-9a-f]");
    private static final Pattern FORMAT = Pattern.compile("(?i)&[0-9a-fk-or]");

    public Medal {
        PermissionIds.require(id);
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > 48) {
            throw new IllegalArgumentException("A medal name has 1 to 48 characters, got: " + name);
        }
        Objects.requireNonNull(symbol, "symbol");
        String visible = FORMAT.matcher(symbol).replaceAll("");
        if (visible.isBlank() || visible.codePointCount(0, visible.length()) > 4 || symbol.length() > MAX_SYMBOL_LENGTH) {
            throw new IllegalArgumentException("A medal symbol shows 1 to 4 characters and may carry & codes, got: " + symbol);
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

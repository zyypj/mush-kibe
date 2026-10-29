package br.com.muxe.core.common.permission.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record Tag(String id, String name, String color, int weight, Optional<String> prefix, boolean decorative) {

    private static final Pattern COLOR = Pattern.compile("&[0-9a-f]");

    public Tag {
        PermissionIds.require(id);
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > 24) {
            throw new IllegalArgumentException("A tag name has 1 to 24 characters, got: " + name);
        }
        color = Objects.requireNonNull(color, "color").toLowerCase(Locale.ROOT);
        if (!COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("A tag color is one & code like &a, got: " + color);
        }
        Objects.requireNonNull(prefix, "prefix");
    }

    public String display() {
        return color + name;
    }

    public String chatPrefix() {
        return prefix.orElse(color + "&l" + name.toUpperCase(Locale.ROOT) + " ");
    }

    public String formatName(String playerName) {
        return chatPrefix() + color + playerName;
    }

    public String permission() {
        return PermissionNode.TAG_PREFIX + id;
    }
}

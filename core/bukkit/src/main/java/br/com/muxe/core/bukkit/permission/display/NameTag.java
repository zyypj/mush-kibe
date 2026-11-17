package br.com.muxe.core.bukkit.permission.display;

import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.Objects;
import java.util.Optional;

public record NameTag(String team, String prefix, String suffix) {

    public static final int MAX_TEAM = 16;
    public static final int MAX_PREFIX = 16;
    public static final int MAX_SUFFIX = 16;
    public static final int STYLE_KEY = 4;

    private static final int STYLE_KEYS = Character.MAX_RADIX * Character.MAX_RADIX * Character.MAX_RADIX * Character.MAX_RADIX;

    private static final int MAX_WEIGHT = 999;
    private static final char COLOR_CHAR = '§';
    private static final String NO_TAG = "-";

    public NameTag {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(prefix, "prefix");
        if (team.isEmpty() || team.length() > MAX_TEAM) {
            throw new IllegalArgumentException("A team name has 1 to 16 characters, got: " + team);
        }
        if (prefix.length() > MAX_PREFIX) {
            throw new IllegalArgumentException("A team prefix has up to 16 characters, got: " + prefix);
        }
        Objects.requireNonNull(suffix, "suffix");
        if (suffix.length() > MAX_SUFFIX) {
            throw new IllegalArgumentException("A team suffix has up to 16 characters, got: " + suffix);
        }
    }

    public NameTag(String team, String prefix) {
        this(team, prefix, "");
    }

    public static NameTag of(Optional<Group> rank, RankDisplay display) {
        String team = order(rank.map(Group::weight).orElse(0))
            + order(display.tag().map(Tag::weight).orElse(0))
            + display.tag().map(Tag::id).orElse(NO_TAG);
        String prefix = fit(Colors.translate(display.prefix()), Colors.translate(display.color()));
        String suffix = cut(Colors.translate(display.emblem()), MAX_SUFFIX);
        return new NameTag(cut(team, MAX_TEAM - STYLE_KEY) + styleKey(prefix + suffix), prefix, suffix);
    }

    static String styleKey(String look) {
        String key = Integer.toString(Math.floorMod(look.hashCode(), STYLE_KEYS), Character.MAX_RADIX);
        return "0".repeat(STYLE_KEY - key.length()) + key;
    }

    static String fit(String prefix, String color) {
        if (prefix.length() + color.length() <= MAX_PREFIX) {
            return prefix + color;
        }
        String cut = cut(prefix, MAX_PREFIX - color.length());
        if (cut.endsWith(String.valueOf(COLOR_CHAR))) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + color;
    }

    private static String order(int weight) {
        return String.format("%03d", MAX_WEIGHT - Math.clamp(weight, 0, MAX_WEIGHT));
    }

    private static String cut(String text, int length) {
        return text.length() > length ? text.substring(0, length) : text;
    }
}

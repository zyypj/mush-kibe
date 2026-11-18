package br.com.muxe.core.bukkit.permission.display;

import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.preference.domain.TagFormat;
import java.util.Locale;
import java.util.Optional;

public final class TagFormatLook {

    public static final String NAME_TEMPLATE = "{prefix}{color}{player}";

    private static final char COLOR_MARK = '&';
    private static final String WHITE = "&f";
    private static final String GRAY = "&7";

    private TagFormatLook() {
    }

    public static RankDisplay apply(TagFormat format, RankDisplay display) {
        if (format == TagFormat.DEFAULT || display.tag().isEmpty() || Colors.strip(Colors.translate(display.prefix())).isBlank()) {
            return display;
        }
        Tag tag = display.tag().get();
        String color = tag.color();
        return switch (format) {
            case DEFAULT -> display;
            case COLOR -> display.withTag(Optional.of(restyle(tag, color, ""))).withEmblem(tail(tag.name()) + (display.emblem().isEmpty() ? "" : color + display.emblem()));
            case BRACKETS_UPPER -> display.withTag(Optional.of(restyle(tag, color, color + "[" + upper(tag.name()) + color + "] ")));
            case BRACKETS -> display.withTag(Optional.of(restyle(tag, color, color + "[" + tag.name() + color + "] ")));
            case DEFAULT_WHITE -> display.withTag(Optional.of(restyle(tag, WHITE, tag.chatPrefix())));
            case DEFAULT_GRAY -> display.withTag(Optional.of(restyle(tag, GRAY, tag.chatPrefix())));
        };
    }

    public static String name(RankDisplay display) {
        return display.chatFormat(NAME_TEMPLATE);
    }

    static String tail(String name) {
        int mark = name.indexOf(COLOR_MARK);
        return mark < 0 ? "" : name.substring(mark);
    }

    static String upper(String name) {
        StringBuilder upper = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char current = name.charAt(index);
            if (current == COLOR_MARK && index + 1 < name.length()) {
                upper.append(current).append(name.charAt(++index));
            } else {
                upper.append(String.valueOf(current).toUpperCase(Locale.ROOT));
            }
        }
        return upper.toString();
    }

    private static Tag restyle(Tag tag, String color, String prefix) {
        return new Tag(tag.id(), tag.name(), color, tag.weight(), Optional.of(prefix), tag.decorative());
    }
}

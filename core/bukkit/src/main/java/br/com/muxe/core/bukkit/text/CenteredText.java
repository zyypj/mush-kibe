package br.com.muxe.core.bukkit.text;

import java.util.Map;

public final class CenteredText {

    public static final int DEFAULT_CENTER = 114;
    public static final int SPACE_WIDTH = 4;
    private static final int DEFAULT_WIDTH = 6;
    private static final char COLOR_CHAR = '§';
    private static final Map<Character, Integer> WIDTHS = Map.ofEntries(
        Map.entry(' ', 4),
        Map.entry('!', 2),
        Map.entry('"', 4),
        Map.entry('\'', 2),
        Map.entry('(', 5),
        Map.entry(')', 5),
        Map.entry('*', 5),
        Map.entry(',', 2),
        Map.entry('.', 2),
        Map.entry(':', 2),
        Map.entry(';', 2),
        Map.entry('<', 5),
        Map.entry('>', 5),
        Map.entry('@', 7),
        Map.entry('I', 4),
        Map.entry('[', 4),
        Map.entry(']', 4),
        Map.entry('`', 3),
        Map.entry('f', 5),
        Map.entry('i', 2),
        Map.entry('k', 5),
        Map.entry('l', 3),
        Map.entry('t', 4),
        Map.entry('{', 5),
        Map.entry('|', 2),
        Map.entry('}', 5),
        Map.entry('~', 7)
    );

    private CenteredText() {
    }

    public static int width(String legacyText) {
        int width = 0;
        boolean bold = false;
        for (int index = 0; index < legacyText.length(); index++) {
            char character = legacyText.charAt(index);
            if (character == COLOR_CHAR && index + 1 < legacyText.length()) {
                char code = Character.toLowerCase(legacyText.charAt(++index));
                if (code == 'l') {
                    bold = true;
                } else if (code == 'r' || isColor(code)) {
                    bold = false;
                }
                continue;
            }
            width += WIDTHS.getOrDefault(character, DEFAULT_WIDTH) + (bold ? 1 : 0);
        }
        return width;
    }

    public static String center(String legacyText, int center) {
        int spaces = (int) Math.round((center - width(legacyText) / 2.0) / SPACE_WIDTH);
        return " ".repeat(Math.max(0, spaces)) + legacyText;
    }

    private static boolean isColor(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }
}

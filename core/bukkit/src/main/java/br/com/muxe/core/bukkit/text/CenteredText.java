package br.com.muxe.core.bukkit.text;

import br.com.muxe.core.common.text.TextWidth;

public final class CenteredText {

    public static final int DEFAULT_CENTER = 114;
    public static final int SPACE_WIDTH = TextWidth.SPACE;

    private CenteredText() {
    }

    public static int width(String legacyText) {
        return TextWidth.of(legacyText);
    }

    public static String center(String legacyText, int center) {
        int spaces = (int) Math.round((center - width(legacyText) / 2.0) / SPACE_WIDTH);
        return " ".repeat(Math.max(0, spaces)) + legacyText;
    }
}

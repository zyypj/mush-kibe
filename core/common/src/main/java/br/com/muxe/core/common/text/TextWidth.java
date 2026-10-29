package br.com.muxe.core.common.text;

public final class TextWidth {

    public static final int SERVER_LIST_WIDTH = 271;
    public static final int SPACE = 4;

    private static final String CODES = "0123456789abcdefklmnor";
    private static final String COLORS = "0123456789abcdefr";
    private static final int DEFAULT_GLYPH = 5;

    private TextWidth() {
    }

    public static int of(String text) {
        int width = 0;
        boolean bold = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (isCodeStart(current) && index + 1 < text.length() && CODES.indexOf(Character.toLowerCase(text.charAt(index + 1))) >= 0) {
                char code = Character.toLowerCase(text.charAt(++index));
                if (code == 'l') {
                    bold = true;
                } else if (COLORS.indexOf(code) >= 0) {
                    bold = false;
                }
                continue;
            }
            width += advance(current) + (bold ? 1 : 0);
        }
        return width;
    }

    public static String center(String text, int area) {
        int free = area - of(text);
        if (free <= 0) {
            return text;
        }
        return " ".repeat((free / 2 + SPACE / 2) / SPACE) + text;
    }

    static int advance(char character) {
        return glyph(character) + 1;
    }

    private static int glyph(char character) {
        return switch (character) {
            case ' ' -> 3;
            case '!', '\'', ',', '.', ':', ';', 'i', '|' -> 1;
            case 'l', '`' -> 2;
            case '"', 'I', 't', '[', ']' -> 3;
            case '(', ')', '*', '<', '>', 'f', 'k', '{', '}' -> 4;
            case '@', '~' -> 6;
            default -> DEFAULT_GLYPH;
        };
    }

    private static boolean isCodeStart(char character) {
        return character == '&' || character == '§';
    }
}

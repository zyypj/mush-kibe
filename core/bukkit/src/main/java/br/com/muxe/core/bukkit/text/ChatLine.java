package br.com.muxe.core.bukkit.text;

public record ChatLine(String text, boolean centered, boolean linked) {

    public static final String CENTER = "<center>";
    public static final String LINK = "<link>";

    public static ChatLine parse(String raw) {
        String text = raw == null ? "" : raw;
        boolean centered = false;
        boolean linked = false;
        boolean changed = true;
        while (changed) {
            changed = false;
            if (text.startsWith(CENTER)) {
                text = text.substring(CENTER.length());
                centered = true;
                changed = true;
            }
            if (text.startsWith(LINK)) {
                text = text.substring(LINK.length());
                linked = true;
                changed = true;
            }
        }
        return new ChatLine(text, centered, linked);
    }

    public String render(int center) {
        return centered ? CenteredText.center(text, center) : text;
    }
}

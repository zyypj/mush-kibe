package br.com.muxe.core.bukkit.hologram;

import br.com.muxe.core.bukkit.effect.PixelShade;
import br.com.muxe.core.bukkit.text.Colors;

public record HologramStyle(String symbol, String highlight, String body, String shadow) {

    public static final String SQUARE = "⬛";

    public static HologramStyle standard() {
        return new HologramStyle(SQUARE, "&f", "&a", "&2");
    }

    public String name(PixelShade shade) {
        String color = switch (shade) {
            case HIGHLIGHT -> highlight;
            case BODY -> body;
            case SHADOW -> shadow;
        };
        return Colors.translate(color + symbol);
    }
}

package br.com.muxe.core.bukkit.effect;

import java.util.ArrayList;
import java.util.List;

public final class ShadedLayout {

    private final PixelFont font;
    private final int letterSpacing;

    public ShadedLayout(PixelFont font, int letterSpacing) {
        if (letterSpacing < 0) {
            throw new IllegalArgumentException("Letter spacing cannot be negative");
        }
        this.font = font;
        this.letterSpacing = letterSpacing;
    }

    public int width(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        return text.length() * (PixelFont.WIDTH + letterSpacing) - letterSpacing;
    }

    public List<ShadedPixel> layout(String text) {
        List<ShadedPixel> pixels = new ArrayList<>();
        for (int index = 0; index < text.length(); index++) {
            int firstColumn = index * (PixelFont.WIDTH + letterSpacing);
            font.glyph(text.charAt(index)).ifPresent(glyph -> {
                for (int line = 0; line < PixelFont.HEIGHT; line++) {
                    for (int column = 0; column < PixelFont.WIDTH; column++) {
                        if (glyph[line][column]) {
                            pixels.add(new ShadedPixel(firstColumn + column, PixelFont.HEIGHT - 1 - line, shadeOf(line)));
                        }
                    }
                }
            });
        }
        return pixels;
    }

    private static PixelShade shadeOf(int line) {
        if (line == 0) {
            return PixelShade.HIGHLIGHT;
        }
        return line == PixelFont.HEIGHT - 1 ? PixelShade.SHADOW : PixelShade.BODY;
    }
}

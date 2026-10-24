package br.com.muxe.core.bukkit.effect;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ParticleText {

    public static final double DEFAULT_PIXEL_SIZE = 0.12;

    private static final int LETTER_SPACING = 1;
    private static final int VIEW_RADIUS = 64;

    private final PixelFont font;
    private final double pixelSize;
    private final ParticleColor color;

    public ParticleText(PixelFont font, double pixelSize, ParticleColor color) {
        if (pixelSize <= 0) {
            throw new IllegalArgumentException("Pixel size must be positive");
        }
        this.font = font;
        this.pixelSize = pixelSize;
        this.color = color;
    }

    public static ParticleText standard(ParticleColor color) {
        return new ParticleText(PixelFont.standard(), DEFAULT_PIXEL_SIZE, color);
    }

    public double height() {
        return PixelFont.HEIGHT * pixelSize;
    }

    public List<Pixel> layout(String text) {
        List<Pixel> pixels = new ArrayList<>();
        int columns = text.length() * (PixelFont.WIDTH + LETTER_SPACING) - LETTER_SPACING;
        double left = -columns * pixelSize / 2;
        for (int index = 0; index < text.length(); index++) {
            int firstColumn = index * (PixelFont.WIDTH + LETTER_SPACING);
            font.glyph(text.charAt(index)).ifPresent(glyph -> {
                for (int row = 0; row < PixelFont.HEIGHT; row++) {
                    for (int column = 0; column < PixelFont.WIDTH; column++) {
                        if (glyph[row][column]) {
                            double x = left + (firstColumn + column) * pixelSize;
                            double y = (PixelFont.HEIGHT - 1 - row) * pixelSize;
                            pixels.add(new Pixel(x, y));
                        }
                    }
                }
            });
        }
        return pixels;
    }

    public void show(Player viewer, Location bottomCenter, String text) {
        double yaw = Math.toRadians(bottomCenter.getYaw());
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        for (Pixel pixel : layout(text)) {
            Location point = bottomCenter.clone().add(rightX * pixel.x(), pixel.y(), rightZ * pixel.x());
            viewer.spigot().playEffect(point, Effect.COLOURED_DUST, 0, 0,
                color.redChannel(), color.greenChannel(), color.blueChannel(), 1f, 0, VIEW_RADIUS);
        }
    }

    public record Pixel(double x, double y) {
    }
}

package br.com.muxe.core.bukkit.hologram;

import br.com.muxe.core.bukkit.effect.PixelFont;
import br.com.muxe.core.bukkit.effect.ShadedLayout;
import br.com.muxe.core.bukkit.effect.ShadedPixel;
import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PixelHolograms {

    public static final double DEFAULT_PIXEL_SIZE = 0.28;
    public static final int DEFAULT_LETTER_SPACING = 2;

    private final ShadedLayout layout;
    private final double pixelSize;
    private final HologramStyle style;
    private final HologramTransport transport;

    public PixelHolograms(PixelFont font, double pixelSize, int letterSpacing, HologramStyle style, HologramTransport transport) {
        if (pixelSize <= 0) {
            throw new IllegalArgumentException("Pixel size must be positive");
        }
        this.layout = new ShadedLayout(font, letterSpacing);
        this.pixelSize = pixelSize;
        this.style = style;
        this.transport = transport;
    }

    public static PixelHolograms standard() {
        return new PixelHolograms(PixelFont.standard(), DEFAULT_PIXEL_SIZE, DEFAULT_LETTER_SPACING, HologramStyle.standard(), PacketHologramTransport.instance());
    }

    public double height() {
        return PixelFont.HEIGHT * pixelSize;
    }

    public List<HologramPixel> layout(Location bottomCenter, String text) {
        double yaw = Math.toRadians(bottomCenter.getYaw());
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double firstColumn = -(layout.width(text) - 1) / 2.0;
        List<HologramPixel> pixels = new ArrayList<>();
        for (ShadedPixel pixel : layout.layout(text)) {
            double side = (firstColumn + pixel.column()) * pixelSize;
            Location point = bottomCenter.clone().add(rightX * side, pixel.row() * pixelSize, rightZ * side);
            pixels.add(new HologramPixel(point, style.name(pixel.shade())));
        }
        return pixels;
    }

    public SpawnedHologram show(Player viewer, Location bottomCenter, String text) {
        return transport.spawn(viewer, layout(bottomCenter, text));
    }
}

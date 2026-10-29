package br.com.muxe.core.bukkit.hologram;

import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import br.com.muxe.core.bukkit.text.Colors;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class TextHolograms {

    public static final double DEFAULT_LINE_SPACING = 0.28;

    private final HologramTransport transport;
    private final double lineSpacing;

    public TextHolograms(HologramTransport transport, double lineSpacing) {
        if (lineSpacing <= 0) {
            throw new IllegalArgumentException("Line spacing must be positive");
        }
        this.transport = transport;
        this.lineSpacing = lineSpacing;
    }

    public static TextHolograms standard() {
        return new TextHolograms(PacketHologramTransport.instance(), DEFAULT_LINE_SPACING);
    }

    public double height(int lines) {
        return Math.max(0, lines - 1) * lineSpacing;
    }

    public List<HologramPixel> layout(Location top, List<String> lines) {
        List<HologramPixel> labels = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            String text = Colors.translate(lines.get(index));
            if (!text.isEmpty()) {
                labels.add(new HologramPixel(top.clone().subtract(0, index * lineSpacing, 0), text));
            }
        }
        return labels;
    }

    public SpawnedHologram show(Player viewer, Location top, List<String> lines) {
        return transport.spawn(viewer, layout(top, lines));
    }
}

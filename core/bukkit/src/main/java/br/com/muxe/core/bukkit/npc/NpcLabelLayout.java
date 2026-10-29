package br.com.muxe.core.bukkit.npc;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.display.LabelStack;
import br.com.muxe.core.bukkit.hologram.HologramPixel;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public record NpcLabelLayout(double bottom, List<Double> gaps, double spacing) {

    public static final NpcLabelLayout STANDARD = new NpcLabelLayout(NpcService.HOLOGRAM_ABOVE_HEAD, List.of(), TextHolograms.DEFAULT_LINE_SPACING);

    public NpcLabelLayout {
        gaps = List.copyOf(gaps);
        if (spacing <= 0 || gaps.stream().anyMatch(gap -> gap <= 0)) {
            throw new IllegalArgumentException("NPC line spacing and gaps must be positive, got " + spacing + " and " + gaps);
        }
    }

    public static NpcLabelLayout from(ConfigurationSection section) {
        if (section == null) {
            return STANDARD;
        }
        return new NpcLabelLayout(
            section.getDouble("bottom", STANDARD.bottom()),
            section.getDoubleList("gaps"),
            section.getDouble("spacing", STANDARD.spacing())
        );
    }

    public List<DisplayLabel> place(List<String> lines) {
        return LabelStack.upward(lines, bottom, gaps, spacing);
    }

    public List<HologramPixel> pixels(Location feet, List<String> lines) {
        List<HologramPixel> pixels = new ArrayList<>(lines.size());
        for (DisplayLabel label : place(lines)) {
            if (!label.text().isEmpty()) {
                pixels.add(new HologramPixel(feet.clone().add(0, label.height(), 0), label.text()));
            }
        }
        return pixels;
    }
}

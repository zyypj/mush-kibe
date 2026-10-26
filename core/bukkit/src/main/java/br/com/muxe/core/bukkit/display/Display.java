package br.com.muxe.core.bukkit.display;

import br.com.muxe.core.bukkit.hologram.HologramPixel;
import br.com.muxe.core.bukkit.text.Colors;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;

public record Display(String id, Location anchor, List<DisplayStand> stands, List<DisplayLabel> labels) {

    public Display {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(anchor, "anchor");
        anchor = anchor.clone();
        stands = List.copyOf(stands);
        labels = List.copyOf(labels);
    }

    public List<HologramPixel> labelPixels() {
        return labels.stream()
            .filter(label -> !Colors.translate(label.text()).isEmpty())
            .map(label -> new HologramPixel(anchor.clone().add(0, label.height(), 0), Colors.translate(label.text())))
            .toList();
    }
}

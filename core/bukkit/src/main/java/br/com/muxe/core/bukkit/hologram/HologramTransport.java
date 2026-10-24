package br.com.muxe.core.bukkit.hologram;

import java.util.List;
import org.bukkit.entity.Player;

public interface HologramTransport {

    SpawnedHologram spawn(Player viewer, List<HologramPixel> pixels);
}

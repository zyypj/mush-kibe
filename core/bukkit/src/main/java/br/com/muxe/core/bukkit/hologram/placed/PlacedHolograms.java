package br.com.muxe.core.bukkit.hologram.placed;

import br.com.muxe.core.bukkit.display.Display;
import br.com.muxe.core.bukkit.display.DisplayService;
import br.com.muxe.core.bukkit.display.LabelStack;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.placement.Reconciler;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import java.util.List;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public final class PlacedHolograms {

    private final Plugin plugin;
    private final double top;
    private final double spacing;
    private final DisplayService displays;
    private final Reconciler<HologramPlacement> shown = new Reconciler<>(HologramPlacement::id);

    public PlacedHolograms(Plugin plugin, double top, double spacing) {
        this.plugin = plugin;
        this.top = top;
        this.spacing = spacing;
        this.displays = DisplayService.start(plugin);
    }

    public void show(List<HologramPlacement> holograms) {
        Reconciler.Changes<HologramPlacement> changes = shown.next(holograms);
        changes.removed().forEach(displays::remove);
        for (HologramPlacement hologram : changes.put()) {
            Optional<Location> anchor = Positions.resolve(hologram.position(), plugin.getServer()::getWorld);
            if (anchor.isPresent()) {
                displays.put(new Display(hologram.id(), anchor.get(), List.of(), LabelStack.downward(hologram.lines(), top, spacing)));
            } else {
                displays.remove(hologram.id());
                plugin.getLogger().warning("Hologram '" + hologram.id() + "' is in world '" + hologram.position().world() + "', which is not loaded");
            }
        }
    }

    public void stop() {
        displays.stop();
    }
}

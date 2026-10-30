package br.com.muxe.minigame.engine.world.server;

import br.com.muxe.minigame.engine.world.cell.Cell;
import br.com.muxe.minigame.engine.world.cell.MatchCells;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class MatchWorld {

    public static final int CELL_CHUNKS = 64;
    public static final int COLUMNS = 32;
    public static final int MARGIN_CHUNKS = 12;

    private static final long SWEEP_PERIOD_TICKS = 100L;
    private static final int SWEEP_MAX_CHUNKS = 64;

    private final Plugin plugin;
    private final MuxeWorlds worlds;
    private final String name;
    private final MatchCells cells = new MatchCells(CELL_CHUNKS, COLUMNS, MARGIN_CHUNKS);
    private World world;
    private BukkitTask sweeper;

    public MatchWorld(Plugin plugin, MuxeWorlds worlds, String name) {
        this.plugin = plugin;
        this.worlds = worlds;
        this.name = name;
    }

    public World start() {
        world = worlds.create(name, cells, false);
        sweeper = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> worlds.sweep(world, SWEEP_MAX_CHUNKS), SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);
        return world;
    }

    public void stop() {
        if (sweeper != null) {
            sweeper.cancel();
        }
        if (world != null) {
            worlds.destroy(world);
            world = null;
        }
    }

    public World world() {
        if (world == null) {
            throw new IllegalStateException("The match world " + name + " is not running");
        }
        return world;
    }

    public boolean fits(MapTemplate map) {
        return cells.fits(map);
    }

    public Cell allocate(MapTemplate map) {
        return cells.allocate(map);
    }

    public void release(Cell cell) {
        worlds.clear(world(), cell);
        cells.release(cell);
    }

    public Optional<Cell> cellAt(Location location) {
        if (world == null || !world.equals(location.getWorld())) {
            return Optional.empty();
        }
        return cells.cellAt(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public int active() {
        return cells.active();
    }

    public boolean contains(World other) {
        return world != null && world.equals(other);
    }
}

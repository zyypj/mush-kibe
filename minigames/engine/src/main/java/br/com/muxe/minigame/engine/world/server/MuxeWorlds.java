package br.com.muxe.minigame.engine.world.server;

import br.com.muxe.minigame.engine.world.cell.Cell;
import br.com.muxe.minigame.engine.world.map.ChunkSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;
import net.minecraft.server.v1_8_R3.ChunkProviderServer;
import net.minecraft.server.v1_8_R3.IChunkLoader;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.Plugin;

public final class MuxeWorlds implements Listener {

    public static final String PREFIX = "muxe_";

    private static final Field CHUNK_LOADER;

    static {
        try {
            CHUNK_LOADER = ChunkProviderServer.class.getDeclaredField("chunkLoader");
            CHUNK_LOADER.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final Plugin plugin;
    private final Map<String, IChunkLoader> pending = new HashMap<>();

    public MuxeWorlds(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static boolean isOurs(String worldName) {
        return worldName.startsWith(PREFIX);
    }

    public World create(String name, ChunkSource source, boolean saveUnchanged) {
        String worldName = PREFIX + name;
        if (plugin.getServer().getWorld(worldName) != null) {
            throw new IllegalStateException("World " + worldName + " is already loaded");
        }
        deleteFolder(folder(worldName));
        pending.put(worldName, new SourceChunkLoader(source, saveUnchanged));
        World world;
        try {
            world = new WorldCreator(worldName).generator(new VoidGenerator()).type(WorldType.FLAT).generateStructures(false).createWorld();
        } finally {
            pending.remove(worldName);
        }
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setStorm(false);
        world.setThundering(false);
        world.setTime(6000);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("randomTickSpeed", "0");
        world.setGameRuleValue("doFireTick", "false");
        world.setGameRuleValue("mobGriefing", "false");
        return world;
    }

    public void destroy(World world) {
        Path folder = world.getWorldFolder().toPath();
        if (!plugin.getServer().unloadWorld(world, false)) {
            plugin.getLogger().warning("Could not unload world " + world.getName());
            return;
        }
        deleteFolder(folder);
    }

    public int clear(World world, Cell cell) {
        int unloaded = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            if (cell.containsChunk(chunk.getX(), chunk.getZ()) && world.unloadChunk(chunk.getX(), chunk.getZ(), false, false)) {
                unloaded++;
            }
        }
        return unloaded;
    }

    public int sweep(World world, int max) {
        int unloaded = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            if (unloaded >= max) {
                break;
            }
            if (world.unloadChunk(chunk.getX(), chunk.getZ(), true, true)) {
                unloaded++;
            }
        }
        return unloaded;
    }

    public void deleteLeftovers() {
        Path container = plugin.getServer().getWorldContainer().toPath();
        List<Path> leftovers;
        try (Stream<Path> listing = Files.list(container)) {
            leftovers = listing
                .filter(Files::isDirectory)
                .filter(path -> isOurs(path.getFileName().toString()))
                .filter(path -> plugin.getServer().getWorld(path.getFileName().toString()) == null)
                .toList();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not list old match worlds", exception);
            return;
        }
        leftovers.forEach(this::deleteFolder);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInit(WorldInitEvent event) {
        IChunkLoader loader = pending.get(event.getWorld().getName());
        if (loader == null) {
            return;
        }
        event.getWorld().setKeepSpawnInMemory(false);
        try {
            CHUNK_LOADER.set(((CraftWorld) event.getWorld()).getHandle().chunkProviderServer, loader);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not install the map loader of " + event.getWorld().getName(), exception);
        }
    }

    private Path folder(String worldName) {
        return plugin.getServer().getWorldContainer().toPath().resolve(worldName);
    }

    private void deleteFolder(Path folder) {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not delete " + folder, exception);
        }
    }
}

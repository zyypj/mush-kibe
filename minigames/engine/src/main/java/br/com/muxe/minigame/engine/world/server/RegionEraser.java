package br.com.muxe.minigame.engine.world.server;

import br.com.muxe.minigame.engine.world.cell.BlockBox;
import br.com.muxe.minigame.engine.world.cell.BlockBox.Span;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PlayerChunkMap;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

public final class RegionEraser {

    private static final int FULL_CHUNK = 0xFFFF;

    private RegionEraser() {
    }

    public static Result erase(World world, BlockBox box) {
        WorldServer handle = ((CraftWorld) world).getHandle();
        long started = System.nanoTime();
        int removed = 0;
        int chunks = 0;
        for (int chunkX = box.minChunkX(); chunkX <= box.maxChunkX(); chunkX++) {
            for (int chunkZ = box.minChunkZ(); chunkZ <= box.maxChunkZ(); chunkZ++) {
                Chunk chunk = handle.getChunkAt(chunkX, chunkZ);
                removeTileEntities(world, chunk, box);
                int changed = clearSections(chunk, box, chunkX, chunkZ);
                if (changed == 0) {
                    continue;
                }
                removed += changed;
                chunks++;
                chunk.initLighting();
                NmsChunks.markModified(chunk);
                resend(handle, chunk);
            }
        }
        return new Result(removed, chunks, (System.nanoTime() - started) / 1_000);
    }

    private static void resend(WorldServer world, Chunk chunk) {
        PlayerChunkMap watchers = world.getPlayerChunkMap();
        PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, true, FULL_CHUNK);
        for (EntityHuman human : world.players) {
            if (human instanceof EntityPlayer player && watchers.a(player, chunk.locX, chunk.locZ)) {
                player.playerConnection.sendPacket(packet);
            }
        }
    }

    private static int clearSections(Chunk chunk, BlockBox box, int chunkX, int chunkZ) {
        ChunkSection[] sections = chunk.getSections();
        int changed = 0;
        for (int index = box.minSection(); index <= box.maxSection(); index++) {
            ChunkSection section = sections[index];
            if (section == null) {
                continue;
            }
            Span span = box.inChunk(chunkX, chunkZ, index);
            if (span.isEmpty()) {
                continue;
            }
            char[] blocks = section.getIdArray();
            int sectionChanged = 0;
            for (int y = span.fromY(); y <= span.toY(); y++) {
                for (int z = span.fromZ(); z <= span.toZ(); z++) {
                    int row = y << 8 | z << 4;
                    for (int x = span.fromX(); x <= span.toX(); x++) {
                        if (blocks[row | x] != 0) {
                            blocks[row | x] = 0;
                            section.getEmittedLightArray().a(x, y, z, 0);
                            sectionChanged++;
                        }
                    }
                }
            }
            if (sectionChanged > 0) {
                section.recalcBlockCounts();
                changed += sectionChanged;
            }
        }
        return changed;
    }

    private static void removeTileEntities(World world, Chunk chunk, BlockBox box) {
        List<BlockPosition> inside = new ArrayList<>();
        for (BlockPosition position : chunk.getTileEntities().keySet()) {
            if (box.contains(position.getX(), position.getY(), position.getZ())) {
                inside.add(position);
            }
        }
        for (BlockPosition position : inside) {
            world.getBlockAt(position.getX(), position.getY(), position.getZ()).setType(Material.AIR, false);
        }
    }

    public record Result(int blocks, int chunks, long micros) {
    }
}

package br.com.muxe.minigame.engine.world.server;

import br.com.muxe.minigame.engine.world.map.ChunkData;
import br.com.muxe.minigame.engine.world.map.SectionData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NibbleArray;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.World.Environment;

final class NmsChunks {

    private static final VarHandle MODIFIED;

    static {
        try {
            MODIFIED = MethodHandles.privateLookupIn(Chunk.class, MethodHandles.lookup()).findVarHandle(Chunk.class, "q", boolean.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private NmsChunks() {
    }

    static Chunk toServer(World world, ChunkData data) {
        Chunk chunk = new Chunk(world, data.x(), data.z());
        ChunkSection[] sections = chunk.getSections();
        boolean sky = hasSky(world);
        for (SectionData section : data.sections()) {
            ChunkSection target = new ChunkSection(section.y() << 4, sky, section.blocks().clone());
            target.a(new NibbleArray(section.blockLight().clone()));
            if (sky) {
                target.b(new NibbleArray(section.skyLight().clone()));
            }
            sections[section.y()] = target;
        }
        chunk.a(data.biomes().clone());
        chunk.a(data.heightMap().clone());
        chunk.d(true);
        chunk.e(true);
        for (byte[] tile : data.tileEntities()) {
            TileEntity entity = TileEntity.c(readNbt(tile));
            if (entity != null) {
                chunk.a(entity);
            }
        }
        return chunk;
    }

    static ChunkData fromServer(Chunk chunk) {
        List<SectionData> sections = new ArrayList<>();
        for (ChunkSection section : chunk.getSections()) {
            if (section == null) {
                continue;
            }
            byte[] sky = section.getSkyLightArray() == null ? new byte[SectionData.NIBBLES] : section.getSkyLightArray().a().clone();
            sections.add(new SectionData(section.getYPosition() >> 4, section.getIdArray().clone(), section.getEmittedLightArray().a().clone(), sky));
        }
        List<byte[]> tiles = new ArrayList<>();
        for (TileEntity entity : chunk.getTileEntities().values()) {
            NBTTagCompound nbt = new NBTTagCompound();
            entity.b(nbt);
            tiles.add(writeNbt(nbt));
        }
        return new ChunkData(chunk.locX, chunk.locZ, sections, chunk.getBiomeIndex().clone(), chunk.q().clone(), tiles);
    }

    static boolean isModified(Chunk chunk) {
        return (boolean) MODIFIED.get(chunk);
    }

    private static boolean hasSky(World world) {
        return world.getWorld().getEnvironment() == Environment.NORMAL;
    }

    private static NBTTagCompound readNbt(byte[] bytes) {
        try {
            return NBTCompressedStreamTools.a(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static byte[] writeNbt(NBTTagCompound nbt) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream stream = new DataOutputStream(bytes)) {
            DataOutput output = stream;
            NBTCompressedStreamTools.a(nbt, output);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return bytes.toByteArray();
    }
}

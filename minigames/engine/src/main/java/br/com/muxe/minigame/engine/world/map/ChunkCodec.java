package br.com.muxe.minigame.engine.world.map;

import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ChunkCodec {

    private static final int SECTION_BYTES = SectionData.BLOCKS * 2 + SectionData.NIBBLES * 2;
    private static final int MAX_RAW_BYTES = 64 * 1024 * 1024;

    private ChunkCodec() {
    }

    public static byte[] compress(ChunkData chunk) {
        byte[] raw = encode(chunk);
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream output = new ByteArrayOutputStream(raw.length / 8 + 64);
            byte[] buffer = new byte[16 * 1024];
            while (!deflater.finished()) {
                output.write(buffer, 0, deflater.deflate(buffer));
            }
            ByteBuffer framed = ByteBuffer.allocate(4 + output.size());
            framed.putInt(raw.length).put(output.toByteArray());
            return framed.array();
        } finally {
            deflater.end();
        }
    }

    public static ChunkData decompress(byte[] compressed) {
        ByteBuffer framed = ByteBuffer.wrap(compressed);
        int rawLength = framed.getInt();
        if (rawLength < 0 || rawLength > MAX_RAW_BYTES) {
            throw new IllegalArgumentException("Corrupted chunk: raw size " + rawLength);
        }
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed, 4, compressed.length - 4);
            byte[] raw = new byte[rawLength];
            int read = 0;
            while (read < rawLength) {
                int count = inflater.inflate(raw, read, rawLength - read);
                if (count == 0 && (inflater.finished() || inflater.needsInput())) {
                    throw new IllegalArgumentException("Corrupted chunk: ended after " + read + " of " + rawLength + " bytes");
                }
                read += count;
            }
            return decode(raw);
        } catch (DataFormatException exception) {
            throw new IllegalArgumentException("Corrupted chunk: " + exception.getMessage(), exception);
        } finally {
            inflater.end();
        }
    }

    static byte[] encode(ChunkData chunk) {
        int size = 4 + 4 + 2 + chunk.sections().size() * SECTION_BYTES + ChunkData.COLUMNS + ChunkData.COLUMNS * 4 + 4;
        for (byte[] tile : chunk.tileEntities()) {
            size += 4 + tile.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(chunk.x()).putInt(chunk.z());
        int mask = 0;
        for (SectionData section : chunk.sections()) {
            mask |= 1 << section.y();
        }
        buffer.putShort((short) mask);
        for (SectionData section : chunk.sections()) {
            buffer.asCharBuffer().put(section.blocks());
            buffer.position(buffer.position() + SectionData.BLOCKS * 2);
            buffer.put(section.blockLight()).put(section.skyLight());
        }
        buffer.put(chunk.biomes());
        buffer.asIntBuffer().put(chunk.heightMap());
        buffer.position(buffer.position() + ChunkData.COLUMNS * 4);
        buffer.putInt(chunk.tileEntities().size());
        for (byte[] tile : chunk.tileEntities()) {
            buffer.putInt(tile.length).put(tile);
        }
        return buffer.array();
    }

    static ChunkData decode(byte[] raw) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            int x = buffer.getInt();
            int z = buffer.getInt();
            int mask = buffer.getShort() & 0xFFFF;
            List<SectionData> sections = new ArrayList<>(Integer.bitCount(mask));
            for (int y = 0; y < 16; y++) {
                if ((mask & 1 << y) == 0) {
                    continue;
                }
                char[] blocks = new char[SectionData.BLOCKS];
                buffer.asCharBuffer().get(blocks);
                buffer.position(buffer.position() + SectionData.BLOCKS * 2);
                byte[] blockLight = new byte[SectionData.NIBBLES];
                byte[] skyLight = new byte[SectionData.NIBBLES];
                buffer.get(blockLight).get(skyLight);
                sections.add(new SectionData(y, blocks, blockLight, skyLight));
            }
            byte[] biomes = new byte[ChunkData.COLUMNS];
            buffer.get(biomes);
            int[] heightMap = new int[ChunkData.COLUMNS];
            buffer.asIntBuffer().get(heightMap);
            buffer.position(buffer.position() + ChunkData.COLUMNS * 4);
            int tiles = buffer.getInt();
            if (tiles < 0 || tiles > buffer.remaining() / 4) {
                throw new IllegalArgumentException("Corrupted chunk: " + tiles + " tile entities");
            }
            List<byte[]> tileEntities = new ArrayList<>(tiles);
            for (int index = 0; index < tiles; index++) {
                int length = buffer.getInt();
                if (length < 0 || length > buffer.remaining()) {
                    throw new IllegalArgumentException("Corrupted chunk: tile entity of " + length + " bytes");
                }
                byte[] tile = new byte[length];
                buffer.get(tile);
                tileEntities.add(tile);
            }
            return new ChunkData(x, z, sections, biomes, heightMap, tileEntities);
        } catch (BufferUnderflowException exception) {
            throw new IllegalArgumentException("Corrupted chunk: data ended early", exception);
        }
    }
}

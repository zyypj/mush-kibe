package br.com.muxe.minigame.engine.world.anvil;

import br.com.muxe.minigame.engine.world.nbt.NbtIo;
import br.com.muxe.minigame.engine.world.nbt.NbtTag.CompoundTag;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public final class RegionFile {

    public static final int SECTOR = 4096;
    public static final int CHUNKS = 1024;

    private static final int GZIP = 1;
    private static final int ZLIB = 2;

    private RegionFile() {
    }

    public static List<CompoundTag> read(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length < SECTOR * 2) {
            return List.of();
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        List<CompoundTag> chunks = new ArrayList<>();
        for (int index = 0; index < CHUNKS; index++) {
            int location = buffer.getInt(index * 4);
            int sector = location >>> 8;
            if (sector == 0 || (location & 0xFF) == 0) {
                continue;
            }
            int start = sector * SECTOR;
            if (start + 5 > bytes.length) {
                throw new IOException(file + " points chunk " + index + " past the end of the file");
            }
            int length = buffer.getInt(start);
            int compression = bytes[start + 4];
            if (length <= 1 || start + 4 + length > bytes.length) {
                throw new IOException(file + " has a chunk " + index + " of invalid length " + length);
            }
            try (DataInputStream input = new DataInputStream(open(new ByteArrayInputStream(bytes, start + 5, length - 1), compression, file))) {
                chunks.add(NbtIo.read(input));
            }
        }
        return chunks;
    }

    private static InputStream open(InputStream raw, int compression, Path file) throws IOException {
        return switch (compression) {
            case GZIP -> new GZIPInputStream(raw);
            case ZLIB -> new InflaterInputStream(raw);
            default -> throw new IOException(file + " uses unknown chunk compression " + compression);
        };
    }
}

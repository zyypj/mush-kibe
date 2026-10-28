package br.com.muxe.core.proxy.geoip.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

final class TarGzFixture {

    private final Map<String, byte[]> entries = new LinkedHashMap<>();

    TarGzFixture file(String name, String content) {
        entries.put(name, content.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    byte[] tar() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entries.forEach((name, content) -> {
            out.writeBytes(header(name, content.length, '0'));
            out.writeBytes(content);
            out.writeBytes(new byte[(512 - content.length % 512) % 512]);
        });
        out.writeBytes(new byte[1024]);
        return out.toByteArray();
    }

    byte[] tarGz() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(tar());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return bytes.toByteArray();
    }

    private static byte[] header(String name, int size, char type) {
        byte[] header = new byte[512];
        put(header, 0, name);
        put(header, 100, "0000644");
        put(header, 124, String.format("%011o", size));
        put(header, 136, String.format("%011o", 0));
        header[156] = (byte) type;
        put(header, 257, "ustar");
        int checksum = 0;
        for (int index = 0; index < 512; index++) {
            checksum += index >= 148 && index < 156 ? ' ' : header[index] & 0xFF;
        }
        put(header, 148, String.format("%06o", checksum));
        return header;
    }

    private static void put(byte[] header, int offset, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, offset, bytes.length);
    }
}

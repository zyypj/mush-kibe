package br.com.muxe.core.proxy.geoip.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class TarArchive {

    private static final int BLOCK = 512;
    private static final char REGULAR_FILE = '0';
    private static final char OLD_REGULAR_FILE = '\0';

    private TarArchive() {
    }

    public static Optional<String> copyFirstEndingWith(InputStream tar, String suffix, OutputStream target) throws IOException {
        byte[] header = new byte[BLOCK];
        while (readFully(tar, header)) {
            if (isEmpty(header)) {
                return Optional.empty();
            }
            String name = name(header);
            long size = size(header);
            char type = (char) header[156];
            boolean regular = type == REGULAR_FILE || type == OLD_REGULAR_FILE;
            if (regular && name.endsWith(suffix)) {
                copy(tar, target, size);
                return Optional.of(name);
            }
            skip(tar, padded(size));
        }
        return Optional.empty();
    }

    private static String name(byte[] header) {
        String name = text(header, 0, 100);
        String prefix = text(header, 345, 155);
        return prefix.isEmpty() ? name : prefix + "/" + name;
    }

    private static long size(byte[] header) throws IOException {
        String octal = text(header, 124, 12).trim();
        if (octal.isEmpty()) {
            return 0;
        }
        try {
            long size = Long.parseLong(octal, 8);
            if (size < 0) {
                throw new IOException("Negative tar entry size");
            }
            return size;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid tar entry size: " + octal, exception);
        }
    }

    private static String text(byte[] header, int offset, int length) {
        int end = offset;
        while (end < offset + length && header[end] != 0) {
            end++;
        }
        return new String(header, offset, end - offset, StandardCharsets.US_ASCII).trim();
    }

    private static boolean isEmpty(byte[] header) {
        for (byte value : header) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static long padded(long size) {
        long remainder = size % BLOCK;
        return remainder == 0 ? size : size + BLOCK - remainder;
    }

    private static void copy(InputStream in, OutputStream out, long size) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long left = size;
        while (left > 0) {
            int read = in.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) {
                throw new IOException("The tar archive ended in the middle of an entry");
            }
            out.write(buffer, 0, read);
            left -= read;
        }
    }

    private static void skip(InputStream in, long bytes) throws IOException {
        long left = bytes;
        byte[] buffer = new byte[8 * 1024];
        while (left > 0) {
            int read = in.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) {
                throw new IOException("The tar archive ended in the middle of an entry");
            }
            left -= read;
        }
    }

    private static boolean readFully(InputStream in, byte[] block) throws IOException {
        int offset = 0;
        while (offset < block.length) {
            int read = in.read(block, offset, block.length - offset);
            if (read < 0) {
                if (offset == 0) {
                    return false;
                }
                throw new IOException("The tar archive ended in the middle of a header");
            }
            offset += read;
        }
        return true;
    }
}

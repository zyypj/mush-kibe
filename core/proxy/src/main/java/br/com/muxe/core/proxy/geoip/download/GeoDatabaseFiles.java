package br.com.muxe.core.proxy.geoip.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.GZIPInputStream;

public final class GeoDatabaseFiles {

    private static final String DATABASE_SUFFIX = ".mmdb";

    private final Path directory;
    private final DatabaseSource source;
    private final Clock clock;
    private final Duration refreshAfter;

    public GeoDatabaseFiles(Path directory, DatabaseSource source, Clock clock, Duration refreshAfter) {
        if (refreshAfter.isNegative() || refreshAfter.isZero()) {
            throw new IllegalArgumentException("GeoIP databases need a positive refresh interval");
        }
        this.directory = directory;
        this.source = source;
        this.clock = clock;
        this.refreshAfter = refreshAfter;
    }

    public Path path(GeoEdition edition) {
        return directory.resolve(edition.fileName());
    }

    public boolean isStale(GeoEdition edition) throws IOException {
        Path file = path(edition);
        if (Files.notExists(file)) {
            return true;
        }
        Instant modified = Files.getLastModifiedTime(file).toInstant();
        return !modified.plus(refreshAfter).isAfter(clock.instant());
    }

    public boolean refreshIfStale(GeoEdition edition) throws IOException {
        if (!isStale(edition)) {
            return false;
        }
        download(edition);
        return true;
    }

    public void download(GeoEdition edition) throws IOException {
        Files.createDirectories(directory);
        Path target = path(edition);
        Path temporary = Files.createTempFile(directory, edition.id(), ".download");
        try {
            try (InputStream archive = source.openTarGz(edition);
                 InputStream tar = new GZIPInputStream(archive);
                 OutputStream out = Files.newOutputStream(temporary)) {
                if (TarArchive.copyFirstEndingWith(tar, DATABASE_SUFFIX, out).isEmpty()) {
                    throw new IOException("The " + edition.id() + " archive has no " + DATABASE_SUFFIX + " file");
                }
            }
            move(temporary, target);
            Files.setLastModifiedTime(target, FileTime.from(clock.instant()));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

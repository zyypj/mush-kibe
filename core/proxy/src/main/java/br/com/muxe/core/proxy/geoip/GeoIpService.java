package br.com.muxe.core.proxy.geoip;

import br.com.muxe.core.proxy.geoip.download.GeoDatabaseFiles;
import br.com.muxe.core.proxy.geoip.download.GeoEdition;
import br.com.muxe.core.proxy.geoip.lookup.GeoLocator;
import br.com.muxe.core.proxy.geoip.lookup.MaxMindGeoLocator;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GeoIpService implements AutoCloseable {

    public static final Duration CHECK_INTERVAL = Duration.ofHours(6);

    private final GeoDatabaseFiles files;
    private final Executor io;
    private final Logger logger;
    private volatile GeoLocator locator = GeoLocator.NONE;
    private ScheduledFuture<?> schedule;
    private boolean closed;

    public GeoIpService(GeoDatabaseFiles files, Executor io, Logger logger) {
        this.files = files;
        this.io = io;
        this.logger = logger;
    }

    public GeoLocator locator() {
        return locator;
    }

    public void start(ScheduledExecutorService scheduler) {
        schedule = scheduler.scheduleWithFixedDelay(() -> io.execute(this::refresh), 0, CHECK_INTERVAL.toMinutes(), TimeUnit.MINUTES);
    }

    public synchronized void refresh() {
        if (closed) {
            return;
        }
        boolean changed = false;
        for (GeoEdition edition : GeoEdition.values()) {
            try {
                if (files.refreshIfStale(edition)) {
                    changed = true;
                    logger.info("GeoIP database " + edition.id() + " downloaded");
                }
            } catch (IOException | RuntimeException exception) {
                logger.log(Level.WARNING, "Could not download the GeoIP database " + edition.id() + ": " + exception.getMessage());
            }
        }
        if (changed || locator == GeoLocator.NONE) {
            reopen();
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (schedule != null) {
            schedule.cancel(false);
        }
        closeQuietly(locator);
        locator = GeoLocator.NONE;
    }

    private void reopen() {
        try {
            MaxMindGeoLocator opened = MaxMindGeoLocator.open(files.path(GeoEdition.CITY), files.path(GeoEdition.ASN));
            if (opened.isEmpty()) {
                return;
            }
            GeoLocator previous = locator;
            locator = opened;
            closeQuietly(previous);
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Could not open the GeoIP databases: " + exception.getMessage());
        }
    }

    private void closeQuietly(GeoLocator previous) {
        if (previous instanceof MaxMindGeoLocator reader) {
            try {
                reader.close();
            } catch (IOException exception) {
                logger.log(Level.FINE, "Could not close a GeoIP database", exception);
            }
        }
    }
}

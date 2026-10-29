package br.com.muxe.core.proxy.geoip;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.proxy.geoip.download.GeoDatabaseFiles;
import br.com.muxe.core.proxy.geoip.lookup.GeoLocator;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeoIpServiceTest {

    @TempDir
    private Path folder;

    @Test
    void anUnreachableDownloadLeavesLocationsUnknownWithoutFailing() {
        AtomicInteger attempts = new AtomicInteger();
        GeoDatabaseFiles files = new GeoDatabaseFiles(folder, edition -> {
            attempts.incrementAndGet();
            throw new IOException("offline");
        }, MutableClock.startingAt("2026-10-28T12:00:00Z"), Duration.ofDays(7));
        GeoIpService service = new GeoIpService(files, Runnable::run, Logger.getLogger("test"));

        service.refresh();

        assertThat(service.locator()).isSameAs(GeoLocator.NONE);
        assertThat(attempts).hasValue(2);
    }

    @Test
    void aClosedServiceStopsRefreshing() {
        AtomicInteger attempts = new AtomicInteger();
        GeoDatabaseFiles files = new GeoDatabaseFiles(folder, edition -> {
            attempts.incrementAndGet();
            throw new IOException("offline");
        }, MutableClock.startingAt("2026-10-28T12:00:00Z"), Duration.ofDays(7));
        GeoIpService service = new GeoIpService(files, Runnable::run, Logger.getLogger("test"));

        service.close();
        service.refresh();

        assertThat(attempts).hasValue(0);
    }
}

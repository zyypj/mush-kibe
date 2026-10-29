package br.com.muxe.core.proxy.geoip.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.testing.MutableClock;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeoDatabaseFilesTest {

    @TempDir
    private Path folder;

    private final MutableClock clock = MutableClock.startingAt("2026-10-28T12:00:00Z");
    private final List<GeoEdition> downloads = new ArrayList<>();
    private String content = "version 1";

    private final DatabaseSource source = edition -> {
        downloads.add(edition);
        return new ByteArrayInputStream(new TarGzFixture()
            .file(edition.id() + "_20261027/LICENSE.txt", "license")
            .file(edition.id() + "_20261027/" + edition.fileName(), content)
            .tarGz());
    };

    @Test
    void aMissingDatabaseIsDownloadedAndExtracted() throws IOException {
        GeoDatabaseFiles files = files();

        assertThat(files.refreshIfStale(GeoEdition.CITY)).isTrue();

        assertThat(files.path(GeoEdition.CITY)).hasContent("version 1");
        assertThat(downloads).containsExactly(GeoEdition.CITY);
        try (Stream<Path> stream = Files.list(folder)) {
            assertThat(stream.map(path -> path.getFileName().toString())).containsExactly("GeoLite2-City.mmdb");
        }
    }

    @Test
    void aDatabaseYoungerThanSevenDaysIsKept() throws IOException {
        GeoDatabaseFiles files = files();
        files.refreshIfStale(GeoEdition.ASN);
        clock.advance(Duration.ofDays(6));
        content = "version 2";

        assertThat(files.refreshIfStale(GeoEdition.ASN)).isFalse();
        assertThat(files.path(GeoEdition.ASN)).hasContent("version 1");
    }

    @Test
    void aDatabaseOlderThanSevenDaysIsReplaced() throws IOException {
        GeoDatabaseFiles files = files();
        files.refreshIfStale(GeoEdition.ASN);
        clock.advance(Duration.ofDays(7));
        content = "version 2";

        assertThat(files.refreshIfStale(GeoEdition.ASN)).isTrue();
        assertThat(files.path(GeoEdition.ASN)).hasContent("version 2");
        assertThat(downloads).hasSize(2);
    }

    @Test
    void aBrokenDownloadKeepsTheOldDatabase() throws IOException {
        files().download(GeoEdition.CITY);
        clock.advance(Duration.ofDays(8));
        GeoDatabaseFiles broken = new GeoDatabaseFiles(folder, edition -> new ByteArrayInputStream(new TarGzFixture().file("README.txt", "x").tarGz()), clock, Duration.ofDays(7));

        assertThatThrownBy(() -> broken.refreshIfStale(GeoEdition.CITY)).isInstanceOf(IOException.class);
        assertThat(broken.path(GeoEdition.CITY)).hasContent("version 1");
        try (Stream<Path> stream = Files.list(folder)) {
            assertThat(stream).hasSize(1);
        }
    }

    private GeoDatabaseFiles files() {
        return new GeoDatabaseFiles(folder, source, clock, Duration.ofDays(7));
    }
}

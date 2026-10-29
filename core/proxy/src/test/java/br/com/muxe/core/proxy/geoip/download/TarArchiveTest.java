package br.com.muxe.core.proxy.geoip.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TarArchiveTest {

    @Test
    void theDatabaseIsFoundAfterTheOtherFilesOfTheArchive() throws IOException {
        byte[] tar = new TarGzFixture()
            .file("GeoLite2-City_20261027/COPYRIGHT.txt", "copyright")
            .file("GeoLite2-City_20261027/LICENSE.txt", "license ".repeat(100))
            .file("GeoLite2-City_20261027/GeoLite2-City.mmdb", "database bytes")
            .tar();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThat(TarArchive.copyFirstEndingWith(new ByteArrayInputStream(tar), ".mmdb", out))
            .contains("GeoLite2-City_20261027/GeoLite2-City.mmdb");
        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("database bytes");
    }

    @Test
    void anArchiveWithoutTheDatabaseFindsNothing() throws IOException {
        byte[] tar = new TarGzFixture().file("README.txt", "nothing here").tar();

        assertThat(TarArchive.copyFirstEndingWith(new ByteArrayInputStream(tar), ".mmdb", new ByteArrayOutputStream())).isEmpty();
    }

    @Test
    void aTruncatedArchiveIsRejected() {
        byte[] tar = new TarGzFixture().file("GeoLite2-ASN.mmdb", "x".repeat(2000)).tar();
        byte[] truncated = Arrays.copyOf(tar, 900);

        assertThatThrownBy(() -> TarArchive.copyFirstEndingWith(new ByteArrayInputStream(truncated), ".mmdb", new ByteArrayOutputStream()))
            .isInstanceOf(IOException.class);
    }
}

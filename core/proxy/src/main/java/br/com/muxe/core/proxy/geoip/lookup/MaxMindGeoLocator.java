package br.com.muxe.core.proxy.geoip.lookup;

import br.com.muxe.core.common.account.domain.AccountLocation;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class MaxMindGeoLocator implements GeoLocator, Closeable {

    private final Optional<DatabaseReader> city;
    private final Optional<DatabaseReader> asn;

    private MaxMindGeoLocator(Optional<DatabaseReader> city, Optional<DatabaseReader> asn) {
        this.city = city;
        this.asn = asn;
    }

    public static MaxMindGeoLocator open(Path cityFile, Path asnFile) throws IOException {
        Optional<DatabaseReader> city = reader(cityFile);
        try {
            return new MaxMindGeoLocator(city, reader(asnFile));
        } catch (IOException exception) {
            if (city.isPresent()) {
                city.get().close();
            }
            throw exception;
        }
    }

    public boolean isEmpty() {
        return city.isEmpty() && asn.isEmpty();
    }

    @Override
    public AccountLocation locate(InetAddress address) throws IOException {
        try {
            Optional<CityResponse> cityResponse = city.isPresent() ? city.get().tryCity(address) : Optional.empty();
            Optional<AsnResponse> asnResponse = asn.isPresent() ? asn.get().tryAsn(address) : Optional.empty();
            return LocationMapper.map(cityResponse, asnResponse);
        } catch (GeoIp2Exception exception) {
            throw new IOException("GeoIP lookup failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (Optional<DatabaseReader> reader : List.of(city, asn)) {
            try {
                if (reader.isPresent()) {
                    reader.get().close();
                }
            } catch (IOException exception) {
                failure = exception;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static Optional<DatabaseReader> reader(Path file) throws IOException {
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        return Optional.of(new DatabaseReader.Builder(file.toFile()).locales(LocationMapper.LOCALES).build());
    }
}

package br.com.muxe.core.proxy.geoip.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class MaxMindDownloads implements DatabaseSource {

    public static final String DEFAULT_ENDPOINT = "https://download.maxmind.com/geoip/databases/";

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final HttpClient client;
    private final String endpoint;
    private final MaxMindCredentials credentials;

    public MaxMindDownloads(HttpClient client, String endpoint, MaxMindCredentials credentials) {
        this.client = client;
        this.endpoint = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        this.credentials = credentials;
    }

    public URI uriFor(GeoEdition edition) {
        return URI.create(endpoint + edition.id() + "/download?suffix=tar.gz");
    }

    @Override
    public InputStream openTarGz(GeoEdition edition) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uriFor(edition))
            .timeout(TIMEOUT)
            .header("Authorization", credentials.basicAuthorization())
            .GET()
            .build();
        HttpResponse<InputStream> response = send(request);
        int status = response.statusCode();
        if (status >= 300 && status < 400) {
            response.body().close();
            String location = response.headers().firstValue("Location")
                .orElseThrow(() -> new IOException("MaxMind answered " + status + " without a Location for " + edition.id()));
            response = send(HttpRequest.newBuilder(uriFor(edition).resolve(location)).timeout(TIMEOUT).GET().build());
        }
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("MaxMind answered " + response.statusCode() + " for " + edition.id() + statusHint(response.statusCode()));
        }
        return response.body();
    }

    private HttpResponse<InputStream> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading a GeoIP database", exception);
        }
    }

    private static String statusHint(int status) {
        return status == 401 || status == 403 ? " (check geoip.account-id and geoip.license-key)" : "";
    }
}

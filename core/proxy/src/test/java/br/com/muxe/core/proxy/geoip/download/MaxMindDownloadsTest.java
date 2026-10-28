package br.com.muxe.core.proxy.geoip.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaxMindDownloadsTest {

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private String endpoint;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/geoip/databases/", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            requests.add(exchange.getRequestURI() + " " + authorization);
            if (!"Basic MTIzNDU2OmxpY2Vuc2UtdmFsdWU=".equals(authorization)) {
                respond(exchange, 401, "unauthorized");
                return;
            }
            exchange.getResponseHeaders().set("Location", "/storage/file.tar.gz?signature=abc");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/storage/", exchange -> {
            requests.add(exchange.getRequestURI() + " " + exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "archive");
        });
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/geoip/databases";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void theArchiveIsDownloadedWithBasicAuthAndTheRedirectWithout() throws IOException {
        MaxMindDownloads downloads = new MaxMindDownloads(HttpClient.newHttpClient(), endpoint, new MaxMindCredentials("123456", "license-value"));

        try (InputStream body = downloads.openTarGz(GeoEdition.CITY)) {
            assertThat(new String(body.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("archive");
        }

        assertThat(requests).containsExactly(
            "/geoip/databases/GeoLite2-City/download?suffix=tar.gz Basic MTIzNDU2OmxpY2Vuc2UtdmFsdWU=",
            "/storage/file.tar.gz?signature=abc null"
        );
    }

    @Test
    void wrongKeysExplainWhatToCheck() {
        MaxMindDownloads downloads = new MaxMindDownloads(HttpClient.newHttpClient(), endpoint, new MaxMindCredentials("123456", "wrong"));

        assertThatThrownBy(() -> downloads.openTarGz(GeoEdition.ASN))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("401")
            .hasMessageContaining("MUXE_MAXMIND_LICENSE_KEY")
            .hasMessageNotContaining("wrong");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}

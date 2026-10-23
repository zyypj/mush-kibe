package br.com.muxe.core.proxy.premium;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MojangPremiumLookupTest {

    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer server;
    private MojangPremiumLookup lookup;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            String name = exchange.getRequestURI().getPath().substring(1);
            int status = switch (name) {
                case "Notch" -> 200;
                case "Ghost" -> 204;
                case "Broken" -> 500;
                case "Slow" -> {
                    awaitRelease();
                    yield 200;
                }
                default -> 404;
            };
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        lookup = new MojangPremiumLookup(HttpClient.newHttpClient(), endpoint, Duration.ofMillis(300), Logger.getLogger("test"));
    }

    @AfterEach
    void stopStub() {
        release.countDown();
        server.stop(0);
    }

    @Test
    void existingProfilesArePremium() {
        assertThat(lookup.lookup("Notch")).isEqualTo(PremiumStatus.PREMIUM);
    }

    @Test
    void missingProfilesAreCracked() {
        assertThat(lookup.lookup("Steve")).isEqualTo(PremiumStatus.CRACKED);
        assertThat(lookup.lookup("Ghost")).isEqualTo(PremiumStatus.CRACKED);
    }

    @Test
    void invalidNamesAreCrackedWithoutAsking() {
        assertThat(lookup.lookup("no spaces!")).isEqualTo(PremiumStatus.CRACKED);
    }

    @Test
    void serverErrorsAndTimeoutsAreUnknown() {
        assertThat(lookup.lookup("Broken")).isEqualTo(PremiumStatus.UNKNOWN);
        assertThat(lookup.lookup("Slow")).isEqualTo(PremiumStatus.UNKNOWN);
    }

    private void awaitRelease() {
        try {
            release.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
